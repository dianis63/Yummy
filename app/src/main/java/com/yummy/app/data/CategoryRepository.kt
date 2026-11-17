package com.yummy.app.data

import com.yummy.app.data.model.CategoryInsert
import com.yummy.app.data.model.CategoryRow
import com.yummy.app.data.model.IdRow
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

object CategoryRepository {
    private val client get() = SupabaseProvider.client

    /** Solo las del usuario: las predeterminadas tienen user_id nulo y quedan fuera. */
    suspend fun own(): List<CategoryRow> =
        client.from("categories")
            .select(Columns.list("id", "name", "icon", "user_id")) {
                filter { eq("user_id", uid()) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    suspend fun create(name: String, icon: String) {
        client.from("categories").insert(CategoryInsert(name, icon))
    }

    /** Cuenta los productos asignados, consumidos incluidos: la foránea los cuenta todos. */
    suspend fun productCount(categoryId: String): Int =
        client.from("products")
            .select(Columns.list("id")) {
                filter { eq("category_id", categoryId) }
            }
            .decodeList<IdRow>()
            .size

    /** La foránea de products lo rechaza si la categoría sigue asignada a algún producto. */
    suspend fun delete(id: String) {
        client.from("categories").delete {
            filter { eq("id", id) }
        }
    }

    private fun uid(): String = AuthRepository.currentUserId() ?: error("Sin sesión")
}
