package com.yummy.app.data

import com.yummy.app.data.model.ShoppingItemInsert
import com.yummy.app.data.model.ShoppingItemRow
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ShoppingRepository {
    private val client get() = SupabaseProvider.client

    /** Los ítems del usuario, los más antiguos primero (RLS ya filtra los ajenos). */
    suspend fun items(): List<ShoppingItemRow> =
        client.from("shopping_items")
            .select(
                Columns.list(
                    "id", "name", "quantity", "unit",
                    "category_id", "is_checked", "source", "created_at"
                )
            ) {
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    /** user_id lo completa la base con auth.uid(). */
    suspend fun add(input: ShoppingItemInsert) {
        client.from("shopping_items").insert(input)
    }

    suspend fun setChecked(id: String, checked: Boolean) {
        val checkedAt: String? = if (checked) nowIso() else null
        client.from("shopping_items").update({
            set("is_checked", checked)
            set("checked_at", checkedAt)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(id: String) {
        client.from("shopping_items").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun clearChecked() {
        client.from("shopping_items").delete {
            filter { eq("is_checked", true) }
        }
    }

    /** Postgrest exige filtro en los delete; la política shopping_own ya acota a los propios. */
    suspend fun clearAll() {
        val uid = AuthRepository.currentUserId() ?: error("Sin sesión")
        client.from("shopping_items").delete {
            filter { eq("user_id", uid) }
        }
    }

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
