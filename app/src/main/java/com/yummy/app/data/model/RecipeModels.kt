package com.yummy.app.data.model

import androidx.annotation.StringRes
import com.yummy.app.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class MealType(val value: String, @StringRes val label: Int) {
    BREAKFAST("desayuno", R.string.meal_breakfast),
    LUNCH("almuerzo", R.string.meal_lunch),
    DINNER("cena", R.string.meal_dinner),
    SNACK("snack", R.string.meal_snack);

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: LUNCH
    }
}

enum class Difficulty(val value: String, @StringRes val label: Int) {
    EASY("facil", R.string.difficulty_easy),
    MEDIUM("media", R.string.difficulty_medium),
    HARD("dificil", R.string.difficulty_hard);

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.value == value } ?: EASY
    }
}

// ---------- Filas que vienen de Supabase ----------

@Serializable
data class RecipeRow(
    val id: String,
    val title: String,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("prep_minutes") val prepMinutes: Int = 15,
    @SerialName("meal_type") val mealType: String = "almuerzo",
    val difficulty: String = "facil",
    val instructions: String? = null
)

/** Resultado de la función recipe_suggestions() */
@Serializable
data class RecipeMatch(
    @SerialName("recipe_id") val recipeId: String,
    val total: Int = 0,
    val available: Int = 0,
    @SerialName("match_percent") val matchPercent: Int = 0
)

@Serializable
data class RecipeIngredientRow(
    val id: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pzs"
)

@Serializable
data class FavoriteRow(@SerialName("recipe_id") val recipeId: String)

@Serializable
data class IdRow(val id: String)

// ---------- Filas que se envían a Supabase ----------

@Serializable
data class RecipeInsert(
    val title: String,
    @SerialName("image_path") val imagePath: String?,
    @SerialName("prep_minutes") val prepMinutes: Int,
    @SerialName("meal_type") val mealType: String,
    val difficulty: String,
    val instructions: String?
)

@Serializable
data class IngredientInsert(
    @SerialName("recipe_id") val recipeId: String,
    val name: String,
    val quantity: Double,
    val unit: String
)

// ---------- Modelos de la UI ----------

data class RecipeSummary(
    val id: String,
    val title: String,
    val imagePath: String?,
    val prepMinutes: Int,
    val mealType: MealType,
    val total: Int,
    val available: Int,
    val matchPercent: Int,
    val isFavorite: Boolean
)

data class IngredientAvailability(
    val name: String,
    val quantity: Double,
    val unit: String,
    val available: Boolean
)

data class RecipeDetail(
    val recipe: RecipeRow,
    val ingredients: List<IngredientAvailability>,
    val isFavorite: Boolean
) {
    val total: Int get() = ingredients.size
    val available: Int get() = ingredients.count { it.available }
    val matchPercent: Int
        get() = if (total == 0) 0 else Math.round(100.0 * available / total).toInt()
}

data class ParsedIngredient(val name: String, val quantity: Double, val unit: String)

data class RecipeDraft(
    val title: String,
    val prepMinutes: Int,
    val mealType: MealType,
    val difficulty: Difficulty,
    val ingredients: List<ParsedIngredient>,
    val instructions: String?
)