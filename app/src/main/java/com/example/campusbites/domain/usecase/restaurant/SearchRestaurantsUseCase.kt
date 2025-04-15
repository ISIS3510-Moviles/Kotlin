package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class SearchRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(query: String): List<RestaurantDomain> {
        val dtos = repository.searchRestaurants(query)

        return dtos.map { dto ->
            RestaurantDomain(
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

                foodTags = dto.foodTagsIds.map { getFoodTagByIdUseCase(it) },
                dietaryTags = dto.dietaryTagsIds.map { getDietaryTagByIdUseCase(it) },

                alertsIds = dto.alertsIds,
                reservationsIds = dto.reservationsIds,
                suscribersIds = dto.suscribersIds,
                visitsIds = dto.visitsIds,
                commentsIds = dto.commentsIds
            )
        }
    }
}
