package com.yummy.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.HomeRepository
import com.yummy.app.data.InventoryRepository
import com.yummy.app.data.model.InventoryFilter
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.data.model.ProductCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_QUANTITY = 99999.0
data class InventoryState(
    val loading: Boolean = true,
    val total: Int = 0,
    val products: List<InventoryProduct> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val filter: InventoryFilter = InventoryFilter.ALL,
    val categoryId: String? = null,
    val search: String = "",
    val showError: Boolean = false
)

class InventoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()
    private var job: Job? = null

    fun refresh() = load(full = true)

    fun onSearch(text: String) {
        _state.update { it.copy(search = text) }
        load(full = false, delayMs = 300)
    }

    fun selectStatus(filter: InventoryFilter) {
        _state.update { it.copy(filter = filter, categoryId = null) }
        load(full = false)
    }

    fun selectCategory(id: String) {
        _state.update { it.copy(filter = InventoryFilter.ALL, categoryId = id) }
        load(full = false)
    }

    fun errorShown() = _state.update { it.copy(showError = false) }


    fun changeQuantity(productId: String, delta: Double) {
        val current = _state.value.products.firstOrNull { it.id == productId } ?: return
        val newQty = Math.round((current.quantity + delta) * 100) / 100.0
        if (newQty < 0.0 || newQty > MAX_QUANTITY) return
        val applied = newQty - current.quantity
        if (applied == 0.0) return

        _state.update { s ->
            if (newQty == 0.0) {
                s.copy(
                    products = s.products.filterNot { it.id == productId },
                    total = (s.total - 1).coerceAtLeast(0)
                )
            } else {
                s.copy(
                    products = s.products.map {
                        if (it.id == productId) it.copy(quantity = newQty) else it
                    }
                )
            }
        }

        viewModelScope.launch {
            try {
                InventoryRepository.adjustQuantity(productId, applied)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(showError = true) }
                load(full = true)
            }
        }
    }

    private fun load(full: Boolean, delayMs: Long = 0) {
        job?.cancel()
        job = viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            try {
                coroutineScope {
                    val s = _state.value
                    val products = async {
                        InventoryRepository.products(s.search.trim(), s.filter, s.categoryId)
                    }
                    val categories = if (full) async { InventoryRepository.categories() } else null
                    val summary = if (full) async { HomeRepository.summary() } else null

                    val list = products.await()
                    val cats = categories?.await()
                    val total = summary?.await()?.inStock
                    _state.update {
                        it.copy(
                            loading = false,
                            products = list,
                            categories = cats ?: it.categories,
                            total = total ?: it.total
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showError = true) }
            }
        }
    }
}