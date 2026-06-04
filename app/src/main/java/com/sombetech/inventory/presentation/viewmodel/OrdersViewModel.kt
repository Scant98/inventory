package com.sombetech.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sombetech.inventory.data.model.InventoryEvent
import com.sombetech.inventory.data.model.Order
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CartItem(val product: Product, val quantity: Int)

data class OrdersUiState(
    val products: List<Product> = emptyList(),
    val orders: List<Order> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val placing: Boolean = false,
    val lastOrder: Order? = null,
    val error: String? = null,
) {
    val cartSubtotal: Double get() = cart.sumOf { it.product.price * it.quantity }
    val cartTax: Double get() = cartSubtotal * 0.1
    val cartTotal: Double get() = cartSubtotal + cartTax
}

class OrdersViewModel(private val repo: InventoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state

    init {
        load()
        viewModelScope.launch {
            repo.inventoryEvents.collect { event ->
                if (event is InventoryEvent.OrderPlaced || event is InventoryEvent.StatsChanged) {
                    loadOrders(); loadProducts()
                }
            }
        }
        observeOnlineStatus()
    }

    fun load() {
        viewModelScope.launch {
            val cachedProducts = repo.getCachedProducts()?.filter { it.stock > 0 }
            val cachedOrders   = repo.getCachedOrders()

            if (cachedProducts != null && cachedOrders != null) {
                _state.update {
                    it.copy(
                        products     = cachedProducts,
                        orders       = cachedOrders,
                        loading      = false,
                        isRefreshing = repo.isOnline.value,
                        error        = null,
                    )
                }
            } else {
                _state.update { it.copy(loading = true, error = null) }
            }

            if (!repo.isOnline.value) {
                _state.update { it.copy(loading = false, isRefreshing = false, isOffline = true) }
                return@launch
            }

            try {
                val products = repo.getProducts().filter { it.stock > 0 }
                val orders   = repo.getOrders()
                _state.update {
                    it.copy(products = products, orders = orders, loading = false, isRefreshing = false)
                }
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(
                        loading      = false,
                        isRefreshing = false,
                        error        = if (s.products.isEmpty()) e.message else null,
                    )
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val p = if (repo.isOnline.value) repo.getProducts() else (repo.getCachedProducts() ?: return@launch)
                _state.update { it.copy(products = p.filter { p -> p.stock > 0 }) }
            } catch (_: Exception) {}
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            try {
                val o = if (repo.isOnline.value) repo.getOrders() else (repo.getCachedOrders() ?: return@launch)
                _state.update { it.copy(orders = o) }
            } catch (_: Exception) {}
        }
    }

    private fun observeOnlineStatus() {
        viewModelScope.launch {
            var prevOnline = repo.isOnline.value
            repo.isOnline.collect { online ->
                _state.update { it.copy(isOffline = !online) }
                if (online && !prevOnline) load()
                prevOnline = online
            }
        }
    }

    fun addToCart(product: Product, quantity: Int) {
        _state.update { s ->
            val existing = s.cart.find { it.product.id == product.id }
            val newQty = (existing?.quantity ?: 0) + quantity
            if (newQty > product.stock) return
            val cart = if (existing != null)
                s.cart.map { if (it.product.id == product.id) it.copy(quantity = newQty) else it }
            else
                s.cart + CartItem(product, quantity)
            s.copy(cart = cart)
        }
    }

    fun removeFromCart(productId: String) =
        _state.update { it.copy(cart = it.cart.filter { c -> c.product.id != productId }) }

    fun incrementCartItem(productId: String) {
        _state.update { s ->
            val item = s.cart.find { it.product.id == productId } ?: return
            if (item.quantity >= item.product.stock) return
            s.copy(cart = s.cart.map { if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it })
        }
    }

    fun decrementCartItem(productId: String) {
        _state.update { s ->
            val item = s.cart.find { it.product.id == productId } ?: return
            if (item.quantity <= 1) {
                s.copy(cart = s.cart.filter { it.product.id != productId })
            } else {
                s.copy(cart = s.cart.map { if (it.product.id == productId) it.copy(quantity = it.quantity - 1) else it })
            }
        }
    }

    fun clearCart() = _state.update { it.copy(cart = emptyList()) }

    fun placeOrder() {
        val items = _state.value.cart
        if (items.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(placing = true, error = null) }
            try {
                val order = repo.createOrder(items.map { it.product.id to it.quantity })
                _state.update { it.copy(placing = false, cart = emptyList(), lastOrder = order) }
                loadOrders(); loadProducts()
            } catch (e: Exception) {
                _state.update { it.copy(placing = false, error = e.message) }
            }
        }
    }

    fun clearLastOrder() = _state.update { it.copy(lastOrder = null) }

    class Factory(private val repo: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = OrdersViewModel(repo) as T
    }
}
