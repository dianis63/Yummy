package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Fila de la tabla locations. user_id nulo = ubicación predeterminada (la ven todos). */
@Serializable
data class LocationRow(
    val id: String,
    val name: String,
    val icon: String = "pin",
    @SerialName("user_id") val userId: String? = null
) {
    val isDefault: Boolean get() = userId == null
}

@Serializable
data class LocationInsert(val name: String)