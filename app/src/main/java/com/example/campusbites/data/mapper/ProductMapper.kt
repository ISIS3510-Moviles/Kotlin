// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import javax.inject.Inject

class ProductMapper @Inject constructor(
    private val getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
) {
    suspend fun mapDtoToDomain(dto: ProductDTO): ProductDomain {
        return ProductDomain(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            price = dto.price,
            photo = dto.photo,
            restaurantId = dto.restaurant_id,
            rating = dto.rating,
            ingredientsIds = dto.ingredientsIds,
            discountsIds = dto.discountsIds,
            commentsIds = dto.commentsIds,
            foodTags = dto.foodTagsIds.mapNotNull { tagId ->
                try { getFoodTagByIdUseCase(tagId) } catch (e: Exception) { null }
            },
            dietaryTags = dto.dietaryTagsIds.mapNotNull { tagId ->
                try { getDietaryTagByIdUseCase(tagId) } catch (e: Exception) { null }
            }
        )
    }
}