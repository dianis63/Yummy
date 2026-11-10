package com.yummy.app.ui.addproduct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.InventoryRepository
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.data.model.ProductCategory
import com.yummy.app.data.model.ProductInput
import com.yummy.app.data.model.StorageLocation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddProductState(
    val loading: Boolean = true,
    val categories: List<ProductCategory> = emptyList(),
    val locations: List<StorageLocation> = emptyList(),
    val categoryId: String? = null,
    val locationId: String? = null,
    val expiryDate: String? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val showLoadError: Boolean = false,
    val showSaveError: Boolean = false
)

class AddProductViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddProductState())
    val state: StateFlow<AddProductState> = _state.asStateFlow()
    private var started = false

    fun start(editing: InventoryProduct?, defaultDate: String) {
        if (started) return
        started = true
        _state.update { it.copy(expiryDate = editing?.expiryDate ?: defaultDate) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val categories = async { InventoryRepository.categories() }
                    val locations = async { InventoryRepository.locations() }
                    val cats = categories.await()
                    val locs = locations.await()
                    _state.update {
                        it.copy(
                            loading = false,
                            categories = cats,
                            locations = locs,
                            categoryId = editing?.categoryId,
                            locationId = editing?.locationId ?: locs.firstOrNull()?.id
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                started = false
                _state.update { it.copy(loading = false, showLoadError = true) }
            }
        }
    }

    fun selectCategory(id: String) = _state.update { it.copy(categoryId = id) }

    fun selectLocation(id: String) = _state.update { it.copy(locationId = id) }

    fun selectDate(iso: String) = _state.update { it.copy(expiryDate = iso) }

    fun save(editingId: String?, input: ProductInput) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (editingId == null) {
                    InventoryRepository.create(input)
                } else {
                    InventoryRepository.update(editingId, input)
                }
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, showSaveError = true) }
            }
        }
    }

    fun loadErrorShown() = _state.update { it.copy(showLoadError = false) }

    fun saveErrorShown() = _state.update { it.copy(showSaveError = false) }
}