package com.example.campusbites.data.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.util.Date
import kotlinx.serialization.Contextual

@Serializable
data class UpdateRestaurantDTO(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("routeIndications") val routeIndications: String? = null,
    @Contextual @SerializedName("openingTime") val openingTime: Date? = null,
    @Contextual @SerializedName("closingTime") val closingTime: Date? = null,
    @SerializedName("opensHolidays") val opensHolidays: Boolean? = null,
    @SerializedName("opensWeekends") val opensWeekends: Boolean? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("overviewPhoto") val overviewPhoto: String? = null,
    @SerializedName("profilePhoto") val profilePhoto: String? = null,
    @SerializedName("photos") val photos: List<String>? = null, // Mapea a List<String>
    @SerializedName("foodTagsIds") val foodTagsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("dietaryTagsIds") val dietaryTagsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("alertsIds") val alertsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("reservationsIds") val reservationsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("suscribersIds") val suscribersIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("visitsIds") val visitsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("commentsIds") val commentsIds: List<String>? = null, // Mapea a List<String>
    @SerializedName("productsIds") val productsIds: List<String>? = null // Mapea a List<String>
)