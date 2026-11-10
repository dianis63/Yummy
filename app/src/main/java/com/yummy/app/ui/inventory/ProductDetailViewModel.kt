package com.yummy.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.InventoryRepository
import com.yummy.app.data.model.InventoryProduct
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailResult { CONSUMED, DELETED }

data class ProductDetailState(
    val product: InventoryProduct? = null,
    val busy: Boolean = false,
    val changed: Boolean = false,
    val result: DetailResult? = null,
    val showError: Boolean = false
)

class ProductDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailState())
    val state: StateFlow<ProductDetailState> = _state.asStateFlow()
    private var started = false

    fun start(product: InventoryProduct) {
        if (started) return
        started = true
        _state.update { it.copy(product = product) }
    }

    fun startWithId(id: String) {
        if (started) return
        started = true
        fetch(id, markChanged = false)
    }

    fun reload() {
        val id = _state.value.product?.id ?: return
        fetch(id, markChanged = true)
    }

    private fun fetch(id: String, markChanged: Boolean) {
        viewModelScope.launch {
            try {
                val p = InventoryRepository.product(id)
                if (p == null) {
                    _state.update { it.copy(showError = true) }
                } else {
                    _state.update { it.copy(product = p, changed = it.changed || markChanged) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(showError = true) }
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value
        val p = current.product ?: return
        if (current.busy) return
        val target = !p.isFavorite
        _state.update { it.copy(product = p.copy(isFavorite = target)) }
        viewModelScope.launch {
            try {
                InventoryRepository.setFavorite(p.id, target)
                _state.update { it.copy(changed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(product = p, showError = true) }
            }
        }
    }

    fun markConsumed() = perform(DetailResult.CONSUMED) { InventoryRepository.markConsumed(it) }

    fun delete() = perform(DetailResult.DELETED) { InventoryRepository.delete(it) }

    fun resultHandled() = _state.update { it.copy(result = null) }

    fun errorShown() = _state.update { it.copy(showError = false) }

    private fun perform(result: DetailResult, action: suspend (String) -> Unit) {
        val current = _state.value
        val p = current.product ?: return
        if (current.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                action(p.id)
                _state.update { it.copy(busy = false, changed = true, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, showError = true) }
            }
        }
    }
}