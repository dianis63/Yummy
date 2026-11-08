package com.yummy.app.data

import com.yummy.app.data.model.Profile
import com.yummy.app.data.model.ProfileStats
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest

object ProfileRepository {
    private val client get() = SupabaseProvider.client

    private fun uid(): String = AuthRepository.currentUserId() ?: error("Sin sesión")

    suspend fun profile(): Profile =
        client.from("profiles").select { filter { eq("id", uid()) } }.decodeSingle()

    suspend fun stats(): ProfileStats =
        client.postgrest.rpc("profile_stats")
            .decodeList<ProfileStats>().firstOrNull() ?: ProfileStats()

    suspend fun setNotifyDays(days: Int) = updateColumn("notify_days_before", days)
    suspend fun setUnits(units: String) = updateColumn("units", units)
    suspend fun setHousehold(size: Int) = updateColumn("household_size", size)

    private suspend inline fun <reified T> updateColumn(column: String, value: T) {
        client.from("profiles").update({ set(column, value) }) {
            filter { eq("id", uid()) }
        }
    }
}