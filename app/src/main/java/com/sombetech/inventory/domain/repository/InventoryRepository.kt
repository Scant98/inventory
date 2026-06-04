package com.sombetech.inventory.domain.repository

import com.sombetech.inventory.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InventoryRepository {
    val inventoryEvents: Flow<InventoryEvent>
    val isOnline: StateFlow<Boolean>

    // Instant cache reads — never throw, return null if nothing cached yet
    fun getCachedProducts(): List<Product>?
    fun getCachedStats(): Stats?
    fun getCachedOrders(): List<Order>?
    fun getCachedTransactions(productId: String?): List<Transaction>?

    // Network calls — may throw on failure
    suspend fun getStats(): Stats
    suspend fun getProducts(): List<Product>
    suspend fun getProduct(id: String): Product
    suspend fun adjustStock(productId: String, quantity: Int, type: String, note: String): Product
    suspend fun getTransactions(productId: String? = null): List<Transaction>
    suspend fun getReport(): Report
    suspend fun getOrders(): List<Order>
    suspend fun createOrder(items: List<Pair<String, Int>>): Order
}
