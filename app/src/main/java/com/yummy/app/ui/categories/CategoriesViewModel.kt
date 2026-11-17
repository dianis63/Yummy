package com.yummy.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.CategoryRepository
import com.yummy.app.data.model.CategoryRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesState(
    val loading: Boolean = true,
    val items: List<CategoryRow> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val showLoadError: Boolean = false,
    val showSaveError: Boolean = false,
    val showDeleteError: Boolean = false,
    val checking: Boolean = false,
    val confirmDelete: CategoryRow? = null,
    val inUseName: String? = null,
    val inUseCount: Int = 0
)

/** Lo comparten la pantalla de Categorías y la hoja "Nueva Categoría". */
class CategoriesViewModel : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state: StateFlow<CategoriesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val list = CategoryRepository.own()
                _state.update { it.copy(loading = false, items = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showLoadError = true) }
            }
        }
    }

    fun add(name: String, icon: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                CategoryRepository.create(name, icon)
                val list = runCatching { CategoryRepository.own() }.getOrNull()
                _state.update {
                    it.copy(saving = false, saved = true, items = list ?: it.items)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, showSaveError = true) }
            }
        }
    }

    /** Antes de preguntar comprueba si la categoría sigue asignada a algún producto. */
    fun requestDelete(item: CategoryRow) {
        if (_state.value.checking) return
        _state.update { it.copy(checking = true) }
        viewModelScope.launch {
            try {
                val used = CategoryRepository.productCount(item.id)
                _state.update {
                    if (used > 0) {
                        it.copy(checking = false, inUseName = item.name, inUseCount = used)
                    } else {
                        it.copy(checking = false, confirmDelete = item)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(checking = false, showDeleteError = true) }
            }
        }
    }

    fun delete(item: CategoryRow) {
        val previous = _state.value.items
        _state.update { s -> s.copy(items = s.items.filterNot { it.id == item.id }) }
        viewModelScope.launch {
            try {
                CategoryRepository.delete(item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // La categoría vuelve a la lista: el borrado no llegó a ocurrir.
                _state.update { it.copy(items = previous, showDeleteError = true) }
            }
        }
    }

    fun confirmHandled() = _state.update { it.copy(confirmDelete = null) }
    fun inUseHandled() = _state.update { it.copy(inUseName = null, inUseCount = 0) }
    fun savedHandled() = _state.update { it.copy(saved = false) }
    fun loadErrorShown() = _state.update { it.copy(showLoadError = false) }
    fun saveErrorShown() = _state.update { it.copy(showSaveError = false) }
    fun deleteErrorShown() = _state.update { it.copy(showDeleteError = false) }
}
