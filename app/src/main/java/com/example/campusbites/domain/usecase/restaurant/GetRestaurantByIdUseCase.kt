package com.example.campusbites.domain.usecase.restaurant

import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class GetRestaurantByIdUseCase @Inject constructor(
    private val repository: RestaurantRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(id: String): RestaurantDomain? {
        val restaurantDTO = repository.getRestaurant(id) ?: return null
        return RestaurantDomain(
            id = restaurantDTO.id,
            name = restaurantDTO.name,
            description = restaurantDTO.description,
            latitude = restaurantDTO.latitude,
            longitude = restaurantDTO.longitude,
            routeIndications = restaurantDTO.routeIndications,
            openingTime = restaurantDTO.openingTime,
            closingTime = restaurantDTO.closingTime,
            opensHolidays = restaurantDTO.opensHolidays,
            opensWeekends = restaurantDTO.opensWeekends,
            isActive = restaurantDTO.isActive,
            rating = restaurantDTO.rating,
            address = restaurantDTO.address,
            phone = restaurantDTO.phone,
            email = restaurantDTO.email,
            overviewPhoto = restaurantDTO.overviewPhoto,
            profilePhoto = restaurantDTO.profilePhoto,
            photos = restaurantDTO.photos,
            foodTags = restaurantDTO.foodTagsIds.map { getFoodTagByIdUseCase(it) },
            dietaryTags = restaurantDTO.dietaryTagsIds.map { getDietaryTagByIdUseCase(it) },
            alertsIds = restaurantDTO.alertsIds,
            reservationsIds = restaurantDTO.reservationsIds,
            suscribersIds = restaurantDTO.suscribersIds,
            visitsIds = restaurantDTO.visitsIds,
            commentsIds = restaurantDTO.commentsIds
        )
    }
}
