package com.sombetech.inventory.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

sealed class PendingOp {
    data class AdjustStock(
        val productId: String,
        val quantity: Int,
        val type: String,
        val note: String,
    ) : PendingOp()

    data class CreateOrder(val items: List<Pair<String, Int>>) : PendingOp()
}

class OfflineQueue(context: Context) {

    private val prefs = context.getSharedPreferences("inv_queue_v1", Context.MODE_PRIVATE)

    fun enqueueAdjustStock(productId: String, quantity: Int, type: String, note: String) {
        val arr = load()
        arr.put(JSONObject().apply {
            put("op", "ADJUST")
            put("productId", productId)
            put("quantity", quantity)
            put("type", type)
            put("note", note)
        })
        save(arr)
    }

    fun enqueueCreateOrder(items: List<Pair<String, Int>>) {
        val arr = load()
        val itemsArr = JSONArray()
        items.forEach { (pId, qty) ->
            itemsArr.put(JSONObject().apply { put("productId", pId); put("quantity", qty) })
        }
        arr.put(JSONObject().apply { put("op", "ORDER"); put("items", itemsArr) })
        save(arr)
    }

    fun drain(): List<PendingOp> {
        val arr = load()
        val ops = mutableListOf<PendingOp>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            when (obj.optString("op")) {
                "ADJUST" -> ops.add(PendingOp.AdjustStock(
                    productId = obj.getString("productId"),
                    quantity  = obj.getInt("quantity"),
                    type      = obj.getString("type"),
                    note      = obj.getString("note"),
                ))
                "ORDER" -> {
                    val raw = obj.getJSONArray("items")
                    val pairs = (0 until raw.length()).map { j ->
                        raw.getJSONObject(j).let { it.getString("productId") to it.getInt("quantity") }
                    }
                    ops.add(PendingOp.CreateOrder(pairs))
                }
            }
        }
        return ops
    }

    fun clear() = prefs.edit().remove("queue").apply()

    fun isEmpty(): Boolean = load().length() == 0

    private fun load(): JSONArray =
        runCatching { JSONArray(prefs.getString("queue", "[]") ?: "[]") }.getOrElse { JSONArray() }

    private fun save(arr: JSONArray) = prefs.edit().putString("queue", arr.toString()).apply()
}
