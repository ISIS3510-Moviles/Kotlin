package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsByRestaurantUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(restaurantId: String): List<ProductDomain> {
        return productRepository.getProductsByRestaurant(restaurantId)
    }
}