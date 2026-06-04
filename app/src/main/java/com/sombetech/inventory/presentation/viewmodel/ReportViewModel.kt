package com.sombetech.inventory.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sombetech.inventory.data.model.Report
import com.sombetech.inventory.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val report: Report? = null,
    val loading: Boolean = true,
    val isOffline: Boolean = false,
    val error: String? = null,
)

class ReportViewModel(private val repo: InventoryRepository) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state

    init {
        load()
        observeOnlineStatus()
    }

    fun load() {
        viewModelScope.launch {
            if (!repo.isOnline.value) {
                _state.update {
                    it.copy(
                        loading   = false,
                        isOffline = true,
                        error     = if (it.report == null) "Connect to load reports" else null,
                    )
                }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            try {
                val report = repo.getReport()
                _state.update { it.copy(report = report, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Failed to load report") }
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

    class Factory(private val repo: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = ReportViewModel(repo) as T
    }
}
