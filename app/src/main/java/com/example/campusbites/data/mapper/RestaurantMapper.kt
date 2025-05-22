// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import android.util.Log
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class RestaurantMapper @Inject constructor(
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend fun mapDtoToDomain(dto: RestaurantDTO): RestaurantDomain {
        return RestaurantDomain(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            latitude = dto.latitude,
            longitude = dto.longitude,
            routeIndications = dto.routeIndications,
            openingTime = dto.openingTime,
            closingTime = dto.closingTime,
            opensHolidays = dto.opensHolidays,
            opensWeekends = dto.opensWeekends,
            isActive = dto.isActive,
            rating = dto.rating,
            address = dto.address,
            phone = dto.phone,
            email = dto.email,
            overviewPhoto = dto.overviewPhoto,
            profilePhoto = dto.profilePhoto,
            photos = dto.photos,
            foodTags = dto.foodTagsIds.mapNotNull { tagId ->
                try { getFoodTagByIdUseCase(tagId) } catch (e: Exception) {
                    Log.w("RestaurantMapper", "Could not fetch food tag $tagId for restaurant ${dto.id}")
                    null
                }
            },
            dietaryTags = dto.dietaryTagsIds.mapNotNull { tagId ->
                try { getDietaryTagByIdUseCase(tagId) } catch (e: Exception) {
                    Log.w("RestaurantMapper", "Could not fetch dietary tag $tagId for restaurant ${dto.id}")
                    null
                }
            },
            alertsIds = dto.alertsIds,
            reservationsIds = dto.reservationsIds,
            suscribersIds = dto.suscribersIds,
            visitsIds = dto.visitsIds,
            commentsIds = dto.commentsIds,
            productsIds = dto.productsIds
        )
    }

    fun createFallbackRestaurant(restaurantId: String, alertIdForContext: String = ""): RestaurantDomain {
        val contextMsg = if (alertIdForContext.isNotBlank()) " (context: alert $alertIdForContext)" else ""
        return RestaurantDomain(
            id = restaurantId.ifBlank { "unknown_restaurant${contextMsg.replace(" ", "_")}" },
            name = "Restaurante desconocido",
            description = "", latitude = 0.0, longitude = 0.0, routeIndications = "",
            openingTime = "", closingTime = "", opensHolidays = false, opensWeekends = false,
            isActive = false, rating = 0.0, address = "", phone = "", email = "",
            overviewPhoto = "", profilePhoto = "", photos = emptyList(), foodTags = emptyList(),
            dietaryTags = emptyList(), alertsIds = emptyList(), reservationsIds = emptyList(),
            suscribersIds = emptyList(), visitsIds = emptyList(), commentsIds = emptyList(),
            productsIds = emptyList()
        )
    }
}