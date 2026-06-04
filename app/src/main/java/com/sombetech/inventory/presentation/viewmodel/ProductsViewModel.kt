package com.sombetech.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sombetech.inventory.data.model.InventoryEvent
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StockFilter { ALL, LOW, OUT }

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val stockFilter: StockFilter = StockFilter.ALL,
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
) {
    val filtered: List<Product> get() {
        val q = query.lowercase()
        return products
            .filter { p ->
                q.isEmpty() ||
                p.displayName.lowercase().contains(q) ||
                p.sku.lowercase().contains(q) ||
                p.size.lowercase().contains(q) ||
                p.color.lowercase().contains(q) ||
                p.productType.lowercase().contains(q)
            }
            .filter { p -> when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.LOW -> p.isLowStock && !p.isOutOfStock
                StockFilter.OUT -> p.isOutOfStock
            }}
    }
}

class ProductsViewModel(private val repo: InventoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProductsUiState())
    val state: StateFlow<ProductsUiState> = _state

    init {
        load()
        viewModelScope.launch {
            repo.inventoryEvents.collect { event ->
                if (event is InventoryEvent.ProductChanged || event is InventoryEvent.StatsChanged) load()
            }
        }
        observeOnlineStatus()
    }

    fun load() {
        viewModelScope.launch {
            val cached = repo.getCachedProducts()

            if (cached != null) {
                _state.update {
                    it.copy(
                        products     = cached,
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
                _state.update { it.copy(products = repo.getProducts(), loading = false, isRefreshing = false) }
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

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setFilter(f: StockFilter) = _state.update { it.copy(stockFilter = f) }

    class Factory(private val repo: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = ProductsViewModel(repo) as T
    }
}
