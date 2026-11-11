package com.yummy.app.ui.recipes

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.R
import com.yummy.app.data.RecipeRepository
import com.yummy.app.data.model.MealType
import com.yummy.app.data.model.RecipeSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecipeFilter(@StringRes val label: Int) {
    ALL(R.string.recipes_filter_all),
    FAVORITES(R.string.recipes_filter_favorites),
    BREAKFAST(R.string.meal_breakfast),
    LUNCH(R.string.meal_lunch),
    DINNER(R.string.meal_dinner),
    SNACK(R.string.meal_snack)
}

data class RecipesState(
    val loading: Boolean = true,
    val all: List<RecipeSummary> = emptyList(),
    val filter: RecipeFilter = RecipeFilter.ALL,
    val showError: Boolean = false
) {
    val visible: List<RecipeSummary>
        get() = when (filter) {
            RecipeFilter.ALL -> all
            RecipeFilter.FAVORITES -> all.filter { it.isFavorite }
            RecipeFilter.BREAKFAST -> all.filter { it.mealType == MealType.BREAKFAST }
            RecipeFilter.LUNCH -> all.filter { it.mealType == MealType.LUNCH }
            RecipeFilter.DINNER -> all.filter { it.mealType == MealType.DINNER }
            RecipeFilter.SNACK -> all.filter { it.mealType == MealType.SNACK }
        }
}

class RecipesViewModel : ViewModel() {

    private val _state = MutableStateFlow(RecipesState())
    val state: StateFlow<RecipesState> = _state.asStateFlow()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                val list = RecipeRepository.recipes()
                _state.update { it.copy(loading = false, all = list) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showError = true) }
            }
        }
    }

    fun selectFilter(filter: RecipeFilter) = _state.update { it.copy(filter = filter) }

    fun errorShown() = _state.update { it.copy(showError = false) }
}