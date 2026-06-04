package com.sombetech.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sombetech.inventory.data.model.InventoryEvent
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.Stats
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: Stats? = null,
    val lowStockProducts: List<Product> = emptyList(),
    val outOfStockProducts: List<Product> = emptyList(),
    val recentAlerts: List<InventoryEvent.LowStockAlert> = emptyList(),
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
)

class DashboardViewModel(private val repo: InventoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state

    init {
        load()
        observeEvents()
        observeOnlineStatus()
    }

    fun load() {
        viewModelScope.launch {
            val cachedProducts = repo.getCachedProducts()
            val cachedStats = repo.getCachedStats()

            if (cachedProducts != null && cachedStats != null) {
                // Show cached data instantly — no full-screen spinner
                _state.update {
                    it.copy(
                        stats              = cachedStats,
                        lowStockProducts   = cachedProducts.filter { p -> p.isLowStock && !p.isOutOfStock },
                        outOfStockProducts = cachedProducts.filter { p -> p.isOutOfStock },
                        loading            = false,
                        isRefreshing       = repo.isOnline.value,
                        error              = null,
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
                val stats    = repo.getStats()
                val products = repo.getProducts()
                _state.update {
                    it.copy(
                        stats              = stats,
                        lowStockProducts   = products.filter { p -> p.isLowStock && !p.isOutOfStock },
                        outOfStockProducts = products.filter { p -> p.isOutOfStock },
                        loading            = false,
                        isRefreshing       = false,
                        error              = null,
                    )
                }
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(
                        loading      = false,
                        isRefreshing = false,
                        // Only surface the error if there's no cached data to show
                        error        = if (s.stats == null) e.message else null,
                    )
                }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repo.inventoryEvents.collect { event ->
                when (event) {
                    is InventoryEvent.ProductChanged,
                    is InventoryEvent.StatsChanged,
                    is InventoryEvent.OrderPlaced -> load()
                    is InventoryEvent.LowStockAlert -> {
                        _state.update { s ->
                            val without = s.recentAlerts.filter { it.productId != event.productId }
                            s.copy(recentAlerts = (listOf(event) + without).take(10))
                        }
                    }
                }
            }
        }
    }

    private fun observeOnlineStatus() {
        viewModelScope.launch {
            var prevOnline = repo.isOnline.value
            repo.isOnline.collect { online ->
                _state.update { it.copy(isOffline = !online) }
                if (online && !prevOnline) load() // refresh when connection restores
                prevOnline = online
            }
        }
    }

    class Factory(private val repo: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            DashboardViewModel(repo) as T
    }
}
