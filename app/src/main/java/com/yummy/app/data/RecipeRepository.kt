package com.yummy.app.data

import com.yummy.app.data.model.FavoriteRow
import com.yummy.app.data.model.IdRow
import com.yummy.app.data.model.IngredientAvailability
import com.yummy.app.data.model.IngredientInsert
import com.yummy.app.data.model.InventoryFilter
import com.yummy.app.data.model.MealType
import com.yummy.app.data.model.RecipeDetail
import com.yummy.app.data.model.RecipeDraft
import com.yummy.app.data.model.RecipeIngredientRow
import com.yummy.app.data.model.RecipeInsert
import com.yummy.app.data.model.RecipeMatch
import com.yummy.app.data.model.RecipeRow
import com.yummy.app.data.model.RecipeSummary
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.Normalizer
import java.util.UUID

object RecipeRepository {
    private val client get() = SupabaseProvider.client
    private const val BUCKET = "recipe-images"

    /** Todas las recetas visibles para el usuario, con su porcentaje de coincidencia y favorito. */
    suspend fun recipes(): List<RecipeSummary> = coroutineScope {
        val rows = async {
            client.from("recipes")
                .select(
                    Columns.list("id", "title", "image_path", "prep_minutes", "meal_type", "difficulty")
                ) { order("created_at", Order.DESCENDING) }
                .decodeList<RecipeRow>()
        }
        val matches = async {
            client.postgrest.rpc("recipe_suggestions").decodeList<RecipeMatch>()
        }
        val favorites = async {
            client.from("recipe_favorites")
                .select(Columns.list("recipe_id"))
                .decodeList<FavoriteRow>()
        }

        val matchById = matches.await().associateBy { it.recipeId }
        val favoriteIds = favorites.await().map { it.recipeId }.toSet()

        rows.await()
            .map { r ->
                val m = matchById[r.id]
                RecipeSummary(
                    id = r.id,
                    title = r.title,
                    imagePath = r.imagePath,
                    prepMinutes = r.prepMinutes,
                    mealType = MealType.from(r.mealType),
                    total = m?.total ?: 0,
                    available = m?.available ?: 0,
                    matchPercent = m?.matchPercent ?: 0,
                    isFavorite = r.id in favoriteIds
                )
            }
            .sortedWith(
                compareByDescending<RecipeSummary> { it.matchPercent }.thenBy { it.prepMinutes }
            )
    }

    /** Receta completa; cada ingrediente indica si existe en el inventario activo del usuario. */
    suspend fun detail(id: String): RecipeDetail? = coroutineScope {
        val recipe = async {
            client.from("recipes")
                .select(
                    Columns.list(
                        "id", "title", "image_path", "prep_minutes",
                        "meal_type", "difficulty", "instructions"
                    )
                ) { filter { eq("id", id) } }
                .decodeSingleOrNull<RecipeRow>()
        }
        val ingredients = async {
            client.from("recipe_ingredients")
                .select(Columns.list("id", "name", "quantity", "unit")) {
                    filter { eq("recipe_id", id) }
                }
                .decodeList<RecipeIngredientRow>()
        }
        val stock = async { InventoryRepository.products("", InventoryFilter.ALL, null) }
        val favorite = async {
            client.from("recipe_favorites")
                .select(Columns.list("recipe_id")) { filter { eq("recipe_id", id) } }
                .decodeList<FavoriteRow>()
                .isNotEmpty()
        }

        val row = recipe.await() ?: return@coroutineScope null
        val inStock = stock.await().map { normalize(it.name) }.toSet()
        RecipeDetail(
            recipe = row,
            ingredients = ingredients.await().map {
                IngredientAvailability(
                    name = it.name,
                    quantity = it.quantity,
                    unit = it.unit,
                    available = normalize(it.name) in inStock
                )
            },
            isFavorite = favorite.await()
        )
    }

    suspend fun setFavorite(recipeId: String, favorite: Boolean) {
        if (favorite) {
            client.from("recipe_favorites").insert(FavoriteRow(recipeId))
        } else {
            client.from("recipe_favorites").delete {
                filter { eq("recipe_id", recipeId) }
            }
        }
    }

    /**
     * Crea la receta con sus ingredientes. La imagen se sube a recipe-images/<user_id>/...
     * (las políticas del bucket exigen que la carpeta sea el id del usuario).
     * Si algo falla se intenta deshacer lo que ya se había creado.
     */
    suspend fun create(draft: RecipeDraft, imageBytes: ByteArray?) {
        val uid = AuthRepository.currentUserId() ?: error("Sin sesión")
        val imagePath: String? =
            if (imageBytes != null) "$uid/${UUID.randomUUID()}.jpg" else null

        try {
            if (imageBytes != null && imagePath != null) {
                client.storage.from(BUCKET).upload(imagePath, imageBytes)
            }
            val recipeId = client.from("recipes")
                .insert(
                    RecipeInsert(
                        title = draft.title,
                        imagePath = imagePath,
                        prepMinutes = draft.prepMinutes,
                        mealType = draft.mealType.value,
                        difficulty = draft.difficulty.value,
                        instructions = draft.instructions
                    )
                ) { select(Columns.list("id")) }
                .decodeSingle<IdRow>().id

            try {
                client.from("recipe_ingredients").insert(
                    draft.ingredients.map {
                        IngredientInsert(recipeId, it.name, it.quantity, it.unit)
                    }
                )
            } catch (e: Exception) {
                runCatching {
                    client.from("recipes").delete { filter { eq("id", recipeId) } }
                }
                throw e
            }
        } catch (e: Exception) {
            if (imagePath != null) {
                runCatching { client.storage.from(BUCKET).delete(imagePath) }
            }
            throw e
        }
    }

    /** Misma comparación que la función SQL: sin tildes y sin mayúsculas. */
    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}