package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryProduct(
    val id: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_icon") val categoryIcon: String? = null,
    @SerialName("location_id") val locationId: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("days_left") val daysLeft: Int? = null,
    @SerialName("expiry_status") val expiryStatus: String = "fresh",
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProductCategory(
    val id: String,
    val name: String,
    val icon: String = "tag"
)

@Serializable
data class StorageLocation(
    val id: String,
    val name: String,
    val icon: String = "pin"
)

@Serializable
data class ProductInput(
    val name: String,
    val quantity: Double,
    val unit: String,
    @SerialName("category_id") val categoryId: String?,
    @SerialName("location_id") val locationId: String?,
    @SerialName("expiry_date") val expiryDate: String?
)

enum class InventoryFilter(val value: String) {
    ALL("all"),
    EXPIRING("expiring"),
    EXPIRED("expired"),
    FAVORITES("favorites")
}