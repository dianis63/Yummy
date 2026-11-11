package com.yummy.app.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.LocationRepository
import com.yummy.app.data.model.LocationRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationsState(
    val loading: Boolean = true,
    val items: List<LocationRow> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val showLoadError: Boolean = false,
    val showSaveError: Boolean = false
)

/** Lo comparten la pantalla de Ubicaciones y la hoja "Nueva Ubicación". */
class LocationsViewModel : ViewModel() {

    private val _state = MutableStateFlow(LocationsState())
    val state: StateFlow<LocationsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val list = LocationRepository.all()
                _state.update { it.copy(loading = false, items = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showLoadError = true) }
            }
        }
    }

    fun add(name: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                LocationRepository.create(name)
                val list = runCatching { LocationRepository.all() }.getOrNull()
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

    fun savedHandled() = _state.update { it.copy(saved = false) }
    fun loadErrorShown() = _state.update { it.copy(showLoadError = false) }
    fun saveErrorShown() = _state.update { it.copy(showSaveError = false) }
}