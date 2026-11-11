package com.yummy.app.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.RecipeRepository
import com.yummy.app.data.model.RecipeDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeDetailState(
    val detail: RecipeDetail? = null,
    val busy: Boolean = false,
    val changed: Boolean = false,
    val showError: Boolean = false
)

class RecipeDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(RecipeDetailState())
    val state: StateFlow<RecipeDetailState> = _state.asStateFlow()
    private var started = false

    fun start(id: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val detail = RecipeRepository.detail(id)
                if (detail == null) {
                    _state.update { it.copy(showError = true) }
                } else {
                    _state.update { it.copy(detail = detail) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                started = false
                _state.update { it.copy(showError = true) }
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value
        val detail = current.detail ?: return
        if (current.busy) return
        val target = !detail.isFavorite
        _state.update { it.copy(detail = detail.copy(isFavorite = target), busy = true) }
        viewModelScope.launch {
            try {
                RecipeRepository.setFavorite(detail.recipe.id, target)
                _state.update { it.copy(busy = false, changed = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(detail = detail, busy = false, showError = true) }
            }
        }
    }

    fun errorShown() = _state.update { it.copy(showError = false) }
}