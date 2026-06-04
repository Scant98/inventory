package com.sombetech.inventory.data.local

import android.content.Context
import com.sombetech.inventory.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class LocalCache(context: Context) {

    private val prefs = context.getSharedPreferences("inv_cache_v1", Context.MODE_PRIVATE)

    // ─── Products ─────────────────────────────────────────────────────────────

    fun saveProducts(list: List<Product>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        edit { putString("products", arr.toString()) }
    }

    fun loadProducts(): List<Product>? =
        prefs.getString("products", null)?.let { json ->
            runCatching { JSONArray(json).toProducts() }.getOrNull()
        }

    // ─── Stats ────────────────────────────────────────────────────────────────

    fun saveStats(stats: Stats) = edit { putString("stats", stats.toJson().toString()) }

    fun loadStats(): Stats? =
        prefs.getString("stats", null)?.let { json ->
            runCatching { JSONObject(json).toStats() }.getOrNull()
        }

    // ─── Orders ───────────────────────────────────────────────────────────────

    fun saveOrders(list: List<Order>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        edit { putString("orders", arr.toString()) }
    }

    fun loadOrders(): List<Order>? =
        prefs.getString("orders", null)?.let { json ->
            runCatching { JSONArray(json).toOrders() }.getOrNull()
        }

    // ─── Transactions ─────────────────────────────────────────────────────────

    fun saveTransactions(productId: String?, list: List<Transaction>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        edit { putString("txn_${productId ?: "all"}", arr.toString()) }
    }

    fun loadTransactions(productId: String?): List<Transaction>? =
        prefs.getString("txn_${productId ?: "all"}", null)?.let { json ->
            runCatching { JSONArray(json).toTransactions() }.getOrNull()
        }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) =
        prefs.edit().apply(block).apply()
}

// ─── Serializers (cache → JSON) ───────────────────────────────────────────────

private fun Product.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("sku", sku)
    put("categoryId", categoryId)
    put("category", JSONObject().apply { put("name", categoryName); put("color", categoryColor) })
    put("brand", brand)
    put("productType", productType)
    put("size", size)
    put("gender", gender)
    put("color", color)
    put("price", price)
    put("cost", cost)
    put("stock", stock)
    put("minStock", minStock)
    put("unit", unit)
    put("description", description)
    put("isLowStock", isLowStock)
    put("isOutOfStock", isOutOfStock)
}

private fun Stats.toJson(): JSONObject = JSONObject().apply {
    put("totalProducts", totalProducts)
    put("totalValue", totalValue)
    put("totalRetailValue", totalRetailValue)
    put("lowStockCount", lowStockCount)
    put("outOfStockCount", outOfStockCount)
    put("totalCategories", totalCategories)
    put("todayOrders", todayOrders)
    put("todayRevenue", todayRevenue)
    val arr = JSONArray()
    weeklyRevenue.forEach { arr.put(it) }
    put("weeklyRevenue", arr)
}

private fun Order.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("orderNumber", orderNumber)
    put("total", total)
    put("itemCount", itemCount)
    put("status", status)
    put("createdAt", createdAt)
}

private fun Transaction.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("productName", productName)
    put("productSku", productSku)
    put("type", type)
    put("quantity", quantity)
    put("note", note)
    put("timestamp", timestamp)
}
