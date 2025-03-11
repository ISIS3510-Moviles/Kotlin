package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(): List<RestaurantDomain> {
        val restaurantsDTO = repository.getRestaurants()
        return restaurantsDTO.map {
            RestaurantDomain(
                id = it.id,
                name = it.name,
                description = it.description,
                latitude = it.latitude,
                longitude = it.longitude,
                routeIndications = it.routeIndications,
                openingTime = it.openingTime,
                closingTime = it.closingTime,
                opensHolidays = it.opensHolidays,
                opensWeekends = it.opensWeekends,
                isActive = it.isActive,
                rating = it.rating,
                address = it.address,
                phone = it.phone,
                email = it.email,
                overviewPhoto = it.overviewPhoto,
                profilePhoto = it.profilePhoto,
                photos = it.photos,
                foodTags = it.foodTagsIds.map { foodTagId -> getFoodTagByIdUseCase(foodTagId) },
                dietaryTags = it.dietaryTagsIds.map { dietaryTagId -> getDietaryTagByIdUseCase(dietaryTagId) },
                alertsIds = it.alertsIds,
                reservationsIds = it.reservationsIds,
                suscribersIds = it.suscribersIds,
                visitsIds = it.visitsIds,
                commentsIds = it.commentsIds
            )
        }
    }
}