package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileName(val name: String = "")

@Serializable
data class InventorySummary(
    val expiring: Int = 0,
    @SerialName("in_stock") val inStock: Int = 0,
    val expired: Int = 0
)

@Serializable
data class ExpiringProduct(
    val id: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "",
    @SerialName("category_icon") val categoryIcon: String? = null,
    @SerialName("location_name") val locationName: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("days_left") val daysLeft: Int? = null,
    @SerialName("expiry_status") val expiryStatus: String = "fresh"
)

@Serializable
data class SuggestedRecipe(
    @SerialName("recipe_id") val recipeId: String,
    val title: String,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("prep_minutes") val prepMinutes: Int = 0,
    val total: Int = 0,
    val available: Int = 0,
    @SerialName("match_percent") val matchPercent: Int = 0
)