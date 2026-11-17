package com.yummy.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.InventoryRepository
import com.yummy.app.data.ProfileRepository
import com.yummy.app.data.ShoppingRepository
import com.yummy.app.data.model.ProductCategory
import com.yummy.app.data.model.ShoppingItemInsert
import com.yummy.app.data.model.ShoppingItemRow
import com.yummy.app.ui.addproduct.UnitOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.yummy.app.data.model.StorageLocation

/** Nombre de la categoría comodín en la base; ahí caen los ítems sin categoría. */
private const val OTHER_CATEGORY = "Otros"

data class ShoppingState(
    val loading: Boolean = true,
    val items: List<ShoppingItemRow> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val units: List<String> = UnitOptions.metric,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val showLoadError: Boolean = false,
    val showSaveError: Boolean = false,
    val showActionError: Boolean = false,
    val locations: List<StorageLocation> = emptyList(),
    val buying: Boolean = false,
    val bought: Boolean = false,
    val showBuyError: Boolean = false
) {
    val total: Int get() = items.size
    val checked: Int get() = items.count { it.isChecked }
    val hasChecked: Boolean get() = checked > 0

    /** Pendientes agrupados por categoría y, al final, la sección de comprados. */
    val listItems: List<ShoppingListItem>
        get() {
            val result = mutableListOf<ShoppingListItem>()
            val fallbackId = categories.firstOrNull { it.name.equals(OTHER_CATEGORY, true) }?.id
            val grouped = items.filterNot { it.isChecked }
                .groupBy { it.categoryId ?: fallbackId }

            categories.forEach { c ->
                val group = grouped[c.id].orEmpty()
                if (group.isNotEmpty()) {
                    result += ShoppingListItem.CategoryHeader(c.id, c.name, c.icon, group.size)
                    result += group.map { ShoppingListItem.Entry(it) }
                }
            }

            // Los que no caen en ninguna categoría conocida van a un "Otros" propio.
            val known = categories.mapTo(mutableSetOf<String?>()) { it.id }
            val loose = grouped.filterKeys { it !in known }.values.flatten()
            if (loose.isNotEmpty()) {
                result += ShoppingListItem.CategoryHeader(OTHER_ID, "", "tag", loose.size)
                result += loose.map { ShoppingListItem.Entry(it) }
            }

            val bought = items.filter { it.isChecked }
            if (bought.isNotEmpty()) {
                result += ShoppingListItem.BoughtHeader
                result += bought.map { ShoppingListItem.Entry(it) }
            }
            return result
        }
}

class ShoppingViewModel : ViewModel() {

    private val _state = MutableStateFlow(ShoppingState())
    val state: StateFlow<ShoppingState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val items = async { ShoppingRepository.items() }
                    val categories = async { InventoryRepository.categories() }
                    val locations = async { InventoryRepository.locations() }
                    val system = async {
                        try {
                            ProfileRepository.profile().units
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            "metric"
                        }
                    }
                    val list = items.await()
                    val cats = categories.await()
                    _state.update {
                        it.copy(
                            loading = false,
                            items = list,
                            categories = cats,
                            locations = locations.await(),
                            units = UnitOptions.forSystem(system.await())
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showLoadError = true) }
            }
        }
    }

    fun add(name: String, quantity: Double, unit: String, categoryId: String?) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                ShoppingRepository.add(ShoppingItemInsert(name, quantity, unit, categoryId))
                val list = runCatching { ShoppingRepository.items() }.getOrNull()
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

    fun toggleChecked(item: ShoppingItemRow) {
        val target = !item.isChecked
        val previous = _state.value.items
        _state.update { s ->
            s.copy(items = s.items.map { if (it.id == item.id) it.copy(isChecked = target) else it })
        }
        viewModelScope.launch {
            try {
                ShoppingRepository.setChecked(item.id, target)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(items = previous, showActionError = true) }
            }
        }
    }

    fun buy(item: ShoppingItemRow, quantity: Double, locationId: String, expiry: String) {
        if (_state.value.buying) return
        _state.update { it.copy(buying = true) }
        viewModelScope.launch {
            try {
                ShoppingRepository.buy(item.id, quantity, locationId, expiry)
                val list = runCatching { ShoppingRepository.items() }.getOrNull()
                _state.update { s ->
                    s.copy(
                        buying = false,
                        bought = true,
                        items = list ?: s.items.map {
                            if (it.id == item.id) it.copy(isChecked = true) else it
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(buying = false, showBuyError = true) }
            }
        }
    }

    fun remove(item: ShoppingItemRow) {
        val previous = _state.value.items
        _state.update { s -> s.copy(items = s.items.filterNot { it.id == item.id }) }
        viewModelScope.launch {
            try {
                ShoppingRepository.delete(item.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(items = previous, showActionError = true) }
            }
        }
    }

    fun clearChecked() = clear(keepPending = true)

    fun clearAll() = clear(keepPending = false)

    private fun clear(keepPending: Boolean) {
        val previous = _state.value.items
        _state.update { s ->
            s.copy(items = if (keepPending) s.items.filterNot { it.isChecked } else emptyList())
        }
        viewModelScope.launch {
            try {
                if (keepPending) ShoppingRepository.clearChecked() else ShoppingRepository.clearAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(items = previous, showActionError = true) }
            }
        }
    }

    fun savedHandled() = _state.update { it.copy(saved = false) }
    fun boughtHandled() = _state.update { it.copy(bought = false) }
    fun buyErrorShown() = _state.update { it.copy(showBuyError = false) }
    fun loadErrorShown() = _state.update { it.copy(showLoadError = false) }
    fun saveErrorShown() = _state.update { it.copy(showSaveError = false) }
    fun actionErrorShown() = _state.update { it.copy(showActionError = false) }
}
