package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Fila de la tabla categories. user_id nulo = categoría predeterminada (la ven todos). */
@Serializable
data class CategoryRow(
    val id: String,
    val name: String,
    val icon: String = "tag",
    @SerialName("user_id") val userId: String? = null
) {
    val isDefault: Boolean get() = userId == null
}

/** user_id lo completa la base con auth.uid(). */
@Serializable
data class CategoryInsert(val name: String, val icon: String)
