package com.sombetech.inventory.data.http

import com.sombetech.inventory.BuildConfig
import com.sombetech.inventory.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
private val JSON = "application/json".toMediaType()

class InventoryApiClient(private val http: OkHttpClient) {

    private val base = BuildConfig.API_URL

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$base$path").get().build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw Exception("HTTP ${r.code} for $path")
            r.body?.string() ?: ""
        }
    }

    private suspend fun post(path: String, body: JSONObject): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$base$path")
            .post(body.toString().toRequestBody(JSON))
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) {
                val msg = r.body?.string() ?: ""
                throw Exception(try { JSONObject(msg).optString("error", "HTTP ${r.code}") } catch (_: Exception) { "HTTP ${r.code}" })
            }
            r.body?.string() ?: ""
        }
    }

    suspend fun getStats(): Stats = JSONObject(get("/stats")).toStats()

    suspend fun getProducts(): List<Product> = JSONArray(get("/products")).toProducts()

    suspend fun getProduct(id: String): Product = JSONObject(get("/products/$id")).toProduct()

    suspend fun adjustStock(
        productId: String,
        quantity: Int,
        type: String,
        note: String,
    ): Product {
        val body = JSONObject().apply {
            put("quantity", quantity)
            put("type", type)
            put("note", note)
        }
        return JSONObject(post("/products/$productId/adjust", body))
            .getJSONObject("product").toProduct()
    }

    suspend fun getReport(): Report =
        JSONObject(get("/reports")).toReport()

    suspend fun getTransactions(productId: String? = null, limit: Int = 50): List<Transaction> {
        val q = buildString {
            if (productId != null) append("productId=$productId&")
            append("limit=$limit")
        }
        return JSONArray(get("/transactions?$q")).toTransactions()
    }

    suspend fun getOrders(limit: Int = 30): List<Order> =
        JSONArray(get("/orders?limit=$limit")).toOrders()

    suspend fun createOrder(items: List<Pair<String, Int>>): Order {
        val arr = org.json.JSONArray()
        items.forEach { (productId, qty) ->
            arr.put(JSONObject().apply { put("productId", productId); put("quantity", qty) })
        }
        return JSONObject(post("/orders", JSONObject().apply { put("items", arr) })).toOrder()
    }
}
