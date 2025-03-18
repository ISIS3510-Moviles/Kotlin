package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository,
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend operator fun invoke(): List<ProductDomain> {
        val productsDTO = repository.getProducts()
        return productsDTO.map {
            ProductDomain(
                id = it.id,
                name = it.name,
                description = it.description,
                price = it.price,
                photo = it.photo,
                restaurantId = it.restaurant_id,
                rating = it.rating,
                ingredientsIds = it.ingredientsIds,
                discountsIds = it.discountsIds,
                commentsIds = it.commentsIds,
                foodTags = it.foodTagsIds.map { foodTagId -> getFoodTagByIdUseCase(foodTagId) },
                dietaryTags = it.dietaryTagsIds.map { dietaryTagId -> getDietaryTagByIdUseCase(dietaryTagId) }
            )
        }
    }

}