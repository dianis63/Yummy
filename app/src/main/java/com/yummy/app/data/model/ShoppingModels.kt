package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Origen del ítem: lo escribió el usuario o lo agregó la base al agotarse un producto. */
const val SOURCE_MANUAL = "manual"
const val SOURCE_AUTO = "auto"

/** Fila de la tabla shopping_items. */
@Serializable
data class ShoppingItemRow(
    val id: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "pzs",
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("is_checked") val isChecked: Boolean = false,
    val source: String = SOURCE_MANUAL,
    @SerialName("created_at") val createdAt: String = ""
) {
    val isAuto: Boolean get() = source == SOURCE_AUTO
}

/** user_id y source los completa la base. La cantidad debe ser mayor que cero. */
@Serializable
data class ShoppingItemInsert(
    val name: String,
    val quantity: Double,
    val unit: String,
    @SerialName("category_id") val categoryId: String?
)
