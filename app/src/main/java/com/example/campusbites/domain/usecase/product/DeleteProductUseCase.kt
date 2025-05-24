package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String): Boolean {
        return productRepository.deleteProduct(productId)
    }
}