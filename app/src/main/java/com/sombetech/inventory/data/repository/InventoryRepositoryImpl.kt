package com.sombetech.inventory.data.repository

import com.sombetech.inventory.BuildConfig
import com.sombetech.inventory.data.http.InventoryApiClient
import com.sombetech.inventory.data.local.LocalCache
import com.sombetech.inventory.data.local.NetworkMonitor
import com.sombetech.inventory.data.local.OfflineQueue
import com.sombetech.inventory.data.local.PendingOp
import com.sombetech.inventory.data.model.*
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID

private const val MAX_WS_RETRIES = 10
private const val BASE_DELAY_MS = 1_000L

class InventoryRepositoryImpl(
    private val api: InventoryApiClient,
    private val okHttpClient: OkHttpClient,
    private val cache: LocalCache,
    private val offlineQueue: OfflineQueue,
    private val networkMonitor: NetworkMonitor,
) : InventoryRepository {

    private val clientId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<InventoryEvent>(extraBufferCapacity = 32)
    override val inventoryEvents: SharedFlow<InventoryEvent> = _events.asSharedFlow()

    override val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private var wsRetries = 0
    private var previouslyOnline = networkMonitor.isOnline.value

    init {
        connectWebSocket()
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                val wentOnline = online && !previouslyOnline
                previouslyOnline = online
                if (wentOnline && !offlineQueue.isEmpty()) drainOfflineQueue()
            }
        }
    }

    // ─── WebSocket ────────────────────────────────────────────────────────────

    private fun connectWebSocket() {
        val request = Request.Builder().url(BuildConfig.WS_URL).build()
        okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                wsRetries = 0
                val handshake = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("clientId", clientId)
                    put("clientType", "android")
                    put("timestamp", System.currentTimeMillis())
                }
                ws.send(handshake.toString())
                if (!offlineQueue.isEmpty()) scope.launch { drainOfflineQueue() }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val msg = JSONObject(text)
                    when (msg.optString("type")) {
                        "PRODUCT_CREATED", "PRODUCT_UPDATED", "STOCK_ADJUSTED" -> {
                            val payload = msg.optString("payload")
                            val productId = if (payload.isNotEmpty()) {
                                JSONObject(payload).optString("id")
                                    .ifEmpty { JSONObject(payload).optJSONObject("product")?.optString("id") ?: "" }
                            } else ""
                            scope.launch { _events.emit(InventoryEvent.ProductChanged(productId)) }
                        }
                        "PRODUCT_DELETED" -> {
                            scope.launch { _events.emit(InventoryEvent.StatsChanged) }
                        }
                        "LOW_STOCK_ALERT" -> {
                            val p = JSONObject(msg.optString("payload"))
                            scope.launch {
                                _events.emit(InventoryEvent.LowStockAlert(
                                    productId   = p.optString("productId"),
                                    productName = p.optString("productName"),
                                    sku         = p.optString("sku"),
                                    stock       = p.optInt("stock"),
                                    minStock    = p.optInt("minStock"),
                                ))
                            }
                        }
                        "ORDER_CREATED" -> {
                            scope.launch {
                                _events.emit(InventoryEvent.OrderPlaced)
                                _events.emit(InventoryEvent.StatsChanged)
                            }
                        }
                        "PING" -> ws.send(JSONObject().apply {
                            put("type", "PONG")
                            put("clientId", clientId)
                            put("clientType", "android")
                            put("timestamp", System.currentTimeMillis())
                        }.toString())
                    }
                } catch (_: Exception) {}
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) = reconnect()
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) = reconnect()
        })
    }

    private fun reconnect() {
        if (wsRetries >= MAX_WS_RETRIES) return
        val delay = BASE_DELAY_MS * (1L shl wsRetries).coerceAtMost(32)
        wsRetries++
        scope.launch { delay(delay); connectWebSocket() }
    }

    // ─── Offline queue drain ──────────────────────────────────────────────────

    private suspend fun drainOfflineQueue() {
        val ops = offlineQueue.drain()
        offlineQueue.clear()
        ops.forEach { op ->
            runCatching {
                when (op) {
                    is PendingOp.AdjustStock -> api.adjustStock(op.productId, op.quantity, op.type, op.note)
                    is PendingOp.CreateOrder -> api.createOrder(op.items)
                }
            }
        }
        // Refresh UI after draining queued operations
        _events.emit(InventoryEvent.StatsChanged)
    }

    // ─── Cache reads ──────────────────────────────────────────────────────────

    override fun getCachedProducts(): List<Product>? = cache.loadProducts()
    override fun getCachedStats(): Stats? = cache.loadStats()
    override fun getCachedOrders(): List<Order>? = cache.loadOrders()
    override fun getCachedTransactions(productId: String?): List<Transaction>? = cache.loadTransactions(productId)

    // ─── Network reads (save to cache on success) ─────────────────────────────

    override suspend fun getStats(): Stats = api.getStats().also { cache.saveStats(it) }

    override suspend fun getProducts(): List<Product> = api.getProducts().also { cache.saveProducts(it) }

    override suspend fun getProduct(id: String): Product = api.getProduct(id)

    override suspend fun getTransactions(productId: String?): List<Transaction> =
        api.getTransactions(productId).also { cache.saveTransactions(productId, it) }

    override suspend fun getReport(): Report = api.getReport()

    override suspend fun getOrders(): List<Order> = api.getOrders().also { cache.saveOrders(it) }

    // ─── Writes ───────────────────────────────────────────────────────────────

    override suspend fun adjustStock(productId: String, quantity: Int, type: String, note: String): Product {
        if (!networkMonitor.isOnline.value) {
            offlineQueue.enqueueAdjustStock(productId, quantity, type, note)
            // Optimistically update cached product so the UI reflects the change immediately
            val cached = cache.loadProducts()?.find { it.id == productId }
                ?: throw Exception("Offline — adjustment queued for sync")
            val delta = if (type == "in") quantity else -quantity
            val newStock = (cached.stock + delta).coerceAtLeast(0)
            val optimistic = cached.copy(
                stock        = newStock,
                isLowStock   = newStock in 1..cached.minStock,
                isOutOfStock = newStock == 0,
            )
            cache.saveProducts(
                (cache.loadProducts() ?: emptyList()).map { if (it.id == productId) optimistic else it }
            )
            return optimistic
        }
        return api.adjustStock(productId, quantity, type, note).also { updated ->
            val products = cache.loadProducts()?.toMutableList()
            if (products != null) {
                val idx = products.indexOfFirst { it.id == productId }
                if (idx >= 0) { products[idx] = updated; cache.saveProducts(products) }
            }
        }
    }

    override suspend fun createOrder(items: List<Pair<String, Int>>): Order {
        if (!networkMonitor.isOnline.value) throw Exception("No internet — connect to place orders")
        return api.createOrder(items)
    }
}
