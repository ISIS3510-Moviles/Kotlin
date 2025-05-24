package com.example.campusbites.domain.usecase.product

import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class UpdateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String, productUpdateData: UpdateProductDTO): ProductDomain {
        return productRepository.updateProduct(productId, productUpdateData)
    }
}