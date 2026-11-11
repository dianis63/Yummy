package com.yummy.app.data

import com.yummy.app.data.model.LocationInsert
import com.yummy.app.data.model.LocationRow
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

object LocationRepository {
    private val client get() = SupabaseProvider.client

    /** Predeterminadas primero y luego las del usuario (RLS ya filtra las ajenas). */
    suspend fun all(): List<LocationRow> =
        client.from("locations")
            .select(Columns.list("id", "name", "icon", "user_id")) {
                order("created_at", Order.ASCENDING)
            }
            .decodeList<LocationRow>()
            .sortedBy { if (it.isDefault) 0 else 1 }

    /** user_id lo completa la base con auth.uid(). */
    suspend fun create(name: String) {
        client.from("locations").insert(LocationInsert(name))
    }
}