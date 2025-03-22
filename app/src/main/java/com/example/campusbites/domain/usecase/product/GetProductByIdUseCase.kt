package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class GetProductByIdUseCase @Inject constructor(
    private val repository: ProductRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(id: String): ProductDomain {
        val product = repository.getProductById(id)
        return ProductDomain(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            photo = product.photo,
            restaurantId = product.restaurant_id,
            rating = product.rating,
            ingredientsIds = product.ingredientsIds,
            discountsIds = product.discountsIds,
            commentsIds = product.commentsIds,
            foodTags = product.foodTagsIds.map { foodTagId -> getFoodTagByIdUseCase(foodTagId) },
            dietaryTags = product.dietaryTagsIds.map { dietaryTagId -> getDietaryTagByIdUseCase(dietaryTagId) }
        )
    }

}