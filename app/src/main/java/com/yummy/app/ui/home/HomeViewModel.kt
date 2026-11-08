package com.yummy.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.HomeRepository
import com.yummy.app.data.model.ExpiringProduct
import com.yummy.app.data.model.InventorySummary
import com.yummy.app.data.model.SuggestedRecipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val loading: Boolean = true,
    val name: String = "",
    val summary: InventorySummary = InventorySummary(),
    val expiring: List<ExpiringProduct> = emptyList(),
    val recipes: List<SuggestedRecipe> = emptyList(),
    val showError: Boolean = false
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                coroutineScope {
                    val name = async { HomeRepository.userName() }
                    val summary = async { HomeRepository.summary() }
                    val expiring = async { HomeRepository.expiring(3) }
                    val recipes = async { HomeRepository.recipes(6) }
                    _state.value = HomeState(
                        loading = false,
                        name = name.await(),
                        summary = summary.await(),
                        expiring = expiring.await(),
                        recipes = recipes.await()
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showError = true) }
            }
        }
    }

    fun errorShown() = _state.update { it.copy(showError = false) }
}