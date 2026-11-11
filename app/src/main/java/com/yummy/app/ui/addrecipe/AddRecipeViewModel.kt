package com.yummy.app.ui.addrecipe

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.RecipeRepository
import com.yummy.app.data.model.Difficulty
import com.yummy.app.data.model.MealType
import com.yummy.app.data.model.ParsedIngredient
import com.yummy.app.data.model.RecipeDraft
import com.yummy.app.data.model.RecipeUnits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddRecipeState(
    val mealType: MealType = MealType.LUNCH,
    val difficulty: Difficulty = Difficulty.EASY,
    val imageUri: Uri? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val showSaveError: Boolean = false
)

/** Una fila del formulario de ingredientes (la cantidad queda como texto mientras se escribe). */
data class IngredientInput(
    val id: Long,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = RecipeUnits.DEFAULT
)

class AddRecipeViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(AddRecipeState())
    val state: StateFlow<AddRecipeState> = _state.asStateFlow()

    private var nextId = 1L
    private val _rows = MutableStateFlow(listOf(IngredientInput(0L)))
    val rows: StateFlow<List<IngredientInput>> = _rows.asStateFlow()

    fun selectMeal(type: MealType) = _state.update { it.copy(mealType = type) }

    fun selectDifficulty(difficulty: Difficulty) = _state.update { it.copy(difficulty = difficulty) }

    fun selectImage(uri: Uri) = _state.update { it.copy(imageUri = uri) }

    fun addRow() = _rows.update { list ->
        if (list.size >= MAX_ROWS) list else list + IngredientInput(nextId++)
    }

    fun removeRow(id: Long) = _rows.update { list ->
        if (list.size <= 1) list else list.filterNot { it.id == id }
    }

    fun updateRow(id: Long, transform: (IngredientInput) -> IngredientInput) = _rows.update { list ->
        list.map { if (it.id == id) transform(it) else it }
    }

    fun save(
        title: String,
        minutes: Int,
        ingredients: List<ParsedIngredient>,
        instructions: String?
    ) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }

        viewModelScope.launch {
            try {
                val bytes = s.imageUri?.let { uri ->
                    withContext(Dispatchers.IO) {
                        ImageUtils.readJpeg(getApplication<Application>(), uri)
                    } ?: error("No se pudo leer la imagen")
                }
                RecipeRepository.create(
                    RecipeDraft(title, minutes, s.mealType, s.difficulty, ingredients, instructions),
                    bytes
                )
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, showSaveError = true) }
            }
        }
    }

    fun saveErrorShown() = _state.update { it.copy(showSaveError = false) }

    private companion object {
        const val MAX_ROWS = 30
    }
}