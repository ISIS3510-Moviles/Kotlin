package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsByRestaurantUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(restaurantId: String): List<ProductDomain> {
        val productsDTO = productRepository.getProductsByRestaurant(restaurantId)
        return productsDTO.map { productDTO ->
            ProductDomain(
                id = productDTO.id,
                name = productDTO.name,
                description = productDTO.description,
                price = productDTO.price,
                photo = productDTO.photo,
                restaurantId = productDTO.restaurant_id,
                rating = productDTO.rating,
                ingredientsIds = productDTO.ingredientsIds,
                discountsIds = productDTO.discountsIds,
                commentsIds = productDTO.commentsIds,
                foodTags = productDTO.foodTagsIds.map { foodTagId -> getFoodTagByIdUseCase(foodTagId) },
                dietaryTags = productDTO.dietaryTagsIds.map { dietaryTagId -> getDietaryTagByIdUseCase(dietaryTagId) }
            )
        }
    }
}
