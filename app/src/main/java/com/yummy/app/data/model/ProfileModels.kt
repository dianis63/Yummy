package com.yummy.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val name: String = "",
    @SerialName("household_size") val householdSize: Int = 1,
    @SerialName("notify_days_before") val notifyDaysBefore: Int = 3,
    val units: String = "metric"
)

@Serializable
data class ProfileStats(
    val products: Int = 0,
    val recipes: Int = 0,
    val favorites: Int = 0
)