package com.yummy.app.data

import com.yummy.app.BuildConfig
import com.yummy.app.data.model.ExpiringProduct
import com.yummy.app.data.model.InventorySummary
import com.yummy.app.data.model.ProfileName
import com.yummy.app.data.model.SuggestedRecipe
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object HomeRepository {
    private val client get() = SupabaseProvider.client

    suspend fun userName(): String {
        val uid = AuthRepository.currentUserId() ?: return ""
        return client.from("profiles")
            .select(Columns.list("name")) { filter { eq("id", uid) } }
            .decodeSingleOrNull<ProfileName>()?.name.orEmpty()
    }

    suspend fun summary(): InventorySummary =
        client.postgrest.rpc("inventory_summary")
            .decodeList<InventorySummary>().firstOrNull() ?: InventorySummary()

    suspend fun expiring(limit: Int): List<ExpiringProduct> =
        client.postgrest.rpc("expiring_products", buildJsonObject { put("p_limit", limit) })
            .decodeList()

    suspend fun recipes(limit: Int): List<SuggestedRecipe> =
        client.postgrest.rpc("suggested_recipes", buildJsonObject { put("p_limit", limit) })
            .decodeList()

    /** URL pública de una imagen del bucket recipe-images. */
    fun recipeImageUrl(path: String): String =
        "${BuildConfig.SUPABASE_URL.trimEnd('/')}/storage/v1/object/public/recipe-images/$path"
}