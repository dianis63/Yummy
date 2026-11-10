package com.yummy.app.data

import com.yummy.app.data.model.InventoryFilter
import com.yummy.app.data.model.InventoryProduct
import com.yummy.app.data.model.ProductCategory
import com.yummy.app.data.model.ProductInput
import com.yummy.app.data.model.StorageLocation
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object InventoryRepository {
    private val client get() = SupabaseProvider.client

    suspend fun products(
        search: String,
        filter: InventoryFilter,
        categoryId: String?
    ): List<InventoryProduct> =
        client.postgrest.rpc("inventory_products", buildJsonObject {
            put("p_search", search)
            put("p_filter", filter.value)
            put("p_category", categoryId)
        }).decodeList()

    suspend fun product(id: String): InventoryProduct? =
        client.postgrest.rpc("inventory_product", buildJsonObject {
            put("p_id", id)
        }).decodeList<InventoryProduct>().firstOrNull()

    suspend fun categories(): List<ProductCategory> =
        client.from("categories")
            .select(Columns.list("id", "name", "icon")) {
                order("created_at", Order.ASCENDING)
            }
            .decodeList<ProductCategory>()
            .sortedBy { if (it.name == "Otros") 1 else 0 }

    suspend fun locations(): List<StorageLocation> {
        val order = listOf("Refrigerador", "Alacena", "Congelador")
        return client.from("locations")
            .select(Columns.list("id", "name", "icon"))
            .decodeList<StorageLocation>()
            .sortedBy { order.indexOf(it.name).takeIf { i -> i >= 0 } ?: order.size }
    }

    suspend fun create(input: ProductInput) {
        client.from("products").insert(input)
    }

    suspend fun update(id: String, input: ProductInput) {
        client.from("products").update(input) {
            filter { eq("id", id) }
        }
    }

    suspend fun adjustQuantity(id: String, delta: Double) {
        client.postgrest.rpc("adjust_product_quantity", buildJsonObject {
            put("p_id", id)
            put("p_delta", delta)
        })
    }

    suspend fun markConsumed(id: String) = updateColumn(id, "status", "consumed")

    suspend fun setFavorite(id: String, favorite: Boolean) =
        updateColumn(id, "is_favorite", favorite)

    suspend fun delete(id: String) {
        client.from("products").delete {
            filter { eq("id", id) }
        }
    }

    private suspend inline fun <reified T> updateColumn(id: String, column: String, value: T) {
        client.from("products").update({ set(column, value) }) {
            filter { eq("id", id) }
        }
    }
}