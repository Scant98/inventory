package com.sombetech.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sombetech.inventory.data.model.InventoryEvent
import com.sombetech.inventory.data.model.Product
import com.sombetech.inventory.data.model.Transaction
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val transactions: List<Transaction> = emptyList(),
    val loading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val adjusting: Boolean = false,
    val adjustSuccess: Boolean = false,
    val error: String? = null,
)

class ProductDetailViewModel(
    private val repo: InventoryRepository,
    private val productId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state

    init {
        load()
        viewModelScope.launch {
            repo.inventoryEvents.collect { event ->
                if (event is InventoryEvent.ProductChanged && event.productId == productId) load()
            }
        }
        observeOnlineStatus()
    }

    fun load() {
        viewModelScope.launch {
            val cachedProduct = repo.getCachedProducts()?.find { it.id == productId }
            val cachedTxns    = repo.getCachedTransactions(productId)

            if (cachedProduct != null) {
                _state.update {
                    it.copy(
                        product      = cachedProduct,
                        transactions = cachedTxns ?: emptyList(),
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
                val product = repo.getProduct(productId)
                val txns    = repo.getTransactions(productId)
                _state.update { it.copy(product = product, transactions = txns, loading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _state.update { s ->
                    s.copy(
                        loading      = false,
                        isRefreshing = false,
                        error        = if (s.product == null) e.message else null,
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

    fun adjustStock(quantity: Int, type: String, note: String) {
        viewModelScope.launch {
            _state.update { it.copy(adjusting = true, adjustSuccess = false, error = null) }
            try {
                val updated = repo.adjustStock(productId, quantity, type, note)
                val txns = if (repo.isOnline.value) repo.getTransactions(productId)
                           else repo.getCachedTransactions(productId) ?: emptyList()
                _state.update {
                    it.copy(product = updated, transactions = txns, adjusting = false, adjustSuccess = true)
                }
            } catch (e: Exception) {
                _state.update { it.copy(adjusting = false, error = e.message) }
            }
        }
    }

    fun clearSuccess() = _state.update { it.copy(adjustSuccess = false) }

    class Factory(
        private val repo: InventoryRepository,
        private val productId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ProductDetailViewModel(repo, productId) as T
    }
}
