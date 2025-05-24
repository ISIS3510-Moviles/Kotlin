package com.example.campusbites.domain.usecase.product

import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class CreateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productData: CreateProductDTO): ProductDomain {
        // El repositorio maneja la lógica online/offline y devuelve ProductDomain
        return productRepository.createProduct(productData)
    }
}