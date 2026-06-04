package com.sombetech.inventory.data.model

import org.json.JSONArray
import org.json.JSONObject

// ─── Product ──────────────────────────────────────────────────────────────────

enum class StockStatus { OK, LOW, OUT }

data class Product(
    val id: String,
    val name: String,          // model name, e.g. "Air Max 270"
    val sku: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColor: String,
    val brand: String,         // e.g. "Nike"
    val productType: String,   // e.g. "Sneakers", "T-Shirt"
    val size: String,          // e.g. "42", "M"
    val gender: String,        // "men" | "women" | "kids" | "unisex"
    val color: String,         // e.g. "Black/White"
    val price: Double,
    val cost: Double,
    val stock: Int,
    val minStock: Int,
    val unit: String,
    val description: String,
    val isLowStock: Boolean,
    val isOutOfStock: Boolean,
) {
    val displayName: String get() = if (brand.isNotBlank()) "$brand $name" else name
    val displayDetail: String get() = buildString {
        if (size.isNotBlank()) append("Sz $size")
        if (color.isNotBlank()) { if (isNotEmpty()) append(" · "); append(color) }
    }

    val stockStatus: StockStatus get() = when {
        isOutOfStock -> StockStatus.OUT
        isLowStock   -> StockStatus.LOW
        else         -> StockStatus.OK
    }

    val stockFraction: Float get() =
        if (minStock == 0) 1f else (stock.toFloat() / minStock).coerceIn(0f, 1f)
}

fun JSONObject.toProduct(): Product {
    val cat = optJSONObject("category")
    return Product(
        id           = getString("id"),
        name         = getString("name"),
        sku          = getString("sku"),
        categoryId   = getString("categoryId"),
        categoryName = cat?.optString("name") ?: "",
        categoryColor= cat?.optString("color") ?: "gray",
        brand        = optString("brand"),
        productType  = optString("productType"),
        size         = optString("size"),
        gender       = optString("gender", "unisex"),
        color        = optString("color"),
        price        = getDouble("price"),
        cost         = getDouble("cost"),
        stock        = getInt("stock"),
        minStock     = getInt("minStock"),
        unit         = getString("unit"),
        description  = optString("description"),
        isLowStock   = optBoolean("isLowStock"),
        isOutOfStock = optBoolean("isOutOfStock"),
    )
}

fun JSONArray.toProducts(): List<Product> =
    (0 until length()).map { getJSONObject(it).toProduct() }

// ─── Stats ────────────────────────────────────────────────────────────────────

data class Stats(
    val totalProducts: Int,
    val totalValue: Double,
    val totalRetailValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val totalCategories: Int,
    val todayOrders: Int,
    val todayRevenue: Double,
    val weeklyRevenue: List<Double>,
)

fun JSONObject.toStats(): Stats {
    val weekly = getJSONArray("weeklyRevenue")
    return Stats(
        totalProducts    = getInt("totalProducts"),
        totalValue       = getDouble("totalValue"),
        totalRetailValue = getDouble("totalRetailValue"),
        lowStockCount    = getInt("lowStockCount"),
        outOfStockCount  = getInt("outOfStockCount"),
        totalCategories  = getInt("totalCategories"),
        todayOrders      = getInt("todayOrders"),
        todayRevenue     = getDouble("todayRevenue"),
        weeklyRevenue    = (0 until weekly.length()).map { weekly.getDouble(it) },
    )
}

// ─── Transaction ──────────────────────────────────────────────────────────────

data class Transaction(
    val id: String,
    val productName: String,
    val productSku: String,
    val type: String,
    val quantity: Int,
    val note: String,
    val timestamp: Long,
)

fun JSONObject.toTransaction() = Transaction(
    id          = getString("id"),
    productName = getString("productName"),
    productSku  = getString("productSku"),
    type        = getString("type"),
    quantity    = getInt("quantity"),
    note        = optString("note"),
    timestamp   = getLong("timestamp"),
)

fun JSONArray.toTransactions(): List<Transaction> =
    (0 until length()).map { getJSONObject(it).toTransaction() }

// ─── Order ────────────────────────────────────────────────────────────────────

data class Order(
    val id: String,
    val orderNumber: String,
    val total: Double,
    val itemCount: Int,
    val status: String,
    val createdAt: Long,
)

fun JSONObject.toOrder(): Order {
    val items = optJSONArray("items")
    return Order(
        id          = getString("id"),
        orderNumber = getString("orderNumber"),
        total       = getDouble("total"),
        itemCount   = optInt("itemCount", items?.length() ?: 0),
        status      = getString("status"),
        createdAt   = getLong("createdAt"),
    )
}

fun JSONArray.toOrders(): List<Order> =
    (0 until length()).map { getJSONObject(it).toOrder() }

// ─── Inventory events (from WebSocket) ───────────────────────────────────────

sealed class InventoryEvent {
    data class ProductChanged(val productId: String) : InventoryEvent()
    data class LowStockAlert(
        val productId: String,
        val productName: String,
        val sku: String,
        val stock: Int,
        val minStock: Int,
    ) : InventoryEvent()
    object OrderPlaced : InventoryEvent()
    object StatsChanged : InventoryEvent()
}
// ─── Report models ────────────────────────────────────────────────────────────

data class PeriodSummary(
    val revenue: Double,
    val orders: Int,
    val unitsSold: Int,
    val avgOrderValue: Double,
)

fun JSONObject.toPeriodSummary() = PeriodSummary(
    revenue       = getDouble("revenue"),
    orders        = getInt("orders"),
    unitsSold     = getInt("unitsSold"),
    avgOrderValue = getDouble("avgOrderValue"),
)

data class DailyRevenue(val date: String, val revenue: Double, val orders: Int)

data class TopProduct(
    val productName: String,
    val brand: String,
    val productType: String,
    val sku: String,
    val unitsSold: Int,
    val revenue: Double,
)

data class CategoryRevenue(
    val name: String,
    val revenue: Double,
    val orders: Int,
    val percentage: Int,
)

data class BrandInventory(
    val brand: String,
    val products: Int,
    val totalStock: Int,
    val costValue: Double,
    val retailValue: Double,
)

data class SalesReport(
    val today: PeriodSummary,
    val thisWeek: PeriodSummary,
    val thisMonth: PeriodSummary,
    val allTime: PeriodSummary,
    val last30Days: List<DailyRevenue>,
    val topProducts: List<TopProduct>,
    val byCategory: List<CategoryRevenue>,
    val revenueGrowth: Int,
)

data class InventorySummary(
    val totalProducts: Int,
    val totalStock: Int,
    val costValue: Double,
    val retailValue: Double,
    val potentialProfit: Double,
    val profitMarginPct: Int,
)

data class StockMovement(val totalReceived: Int, val totalSold: Int, val totalAdjusted: Int)

data class InventoryReport(
    val summary: InventorySummary,
    val byCategory: List<CategoryRevenue>,
    val byBrand: List<BrandInventory>,
    val stockMovement: StockMovement,
)

data class Report(
    val sales: SalesReport,
    val inventory: InventoryReport,
    val generatedAt: Long,
)

fun JSONObject.toReport(): Report {
    val s = getJSONObject("sales")
    val inv = getJSONObject("inventory")

    fun JSONObject.periodSummary() = toPeriodSummary()

    val last30 = s.getJSONArray("last30Days").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).let { d -> DailyRevenue(d.getString("date"), d.getDouble("revenue"), d.getInt("orders")) } }
    }
    val topProds = s.getJSONArray("topProducts").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).let { p ->
            TopProduct(p.getString("productName"), p.optString("brand"), p.optString("productType"), p.getString("sku"), p.getInt("unitsSold"), p.getDouble("revenue"))
        }}
    }
    val salesByCat = s.getJSONArray("byCategory").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).let { c ->
            CategoryRevenue(c.getString("name"), c.getDouble("revenue"), c.getInt("orders"), c.getInt("percentage"))
        }}
    }
    val invByCat = inv.getJSONArray("byCategory").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).let { c ->
            CategoryRevenue(c.getString("name"), c.getDouble("retailValue"), 0, 0)
        }}
    }
    val byBrand = inv.getJSONArray("byBrand").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).let { b ->
            BrandInventory(b.getString("brand"), b.getInt("products"), b.getInt("totalStock"), b.getDouble("costValue"), b.getDouble("retailValue"))
        }}
    }
    val sm = inv.getJSONObject("stockMovement")
    val summ = inv.getJSONObject("summary")

    return Report(
        sales = SalesReport(
            today        = s.getJSONObject("today").periodSummary(),
            thisWeek     = s.getJSONObject("thisWeek").periodSummary(),
            thisMonth    = s.getJSONObject("thisMonth").periodSummary(),
            allTime      = s.getJSONObject("allTime").periodSummary(),
            last30Days   = last30,
            topProducts  = topProds,
            byCategory   = salesByCat,
            revenueGrowth= s.getInt("revenueGrowth"),
        ),
        inventory = InventoryReport(
            summary = InventorySummary(
                totalProducts  = summ.getInt("totalProducts"),
                totalStock     = summ.getInt("totalStock"),
                costValue      = summ.getDouble("costValue"),
                retailValue    = summ.getDouble("retailValue"),
                potentialProfit= summ.getDouble("potentialProfit"),
                profitMarginPct= summ.getInt("profitMarginPct"),
            ),
            byCategory     = invByCat,
            byBrand        = byBrand,
            stockMovement  = StockMovement(sm.getInt("totalReceived"), sm.getInt("totalSold"), sm.getInt("totalAdjusted")),
        ),
        generatedAt = getLong("generatedAt"),
    )
}