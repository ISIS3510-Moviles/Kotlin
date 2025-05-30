package com.example.campusbites.domain.usecase.product

import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(): List<ProductDomain> {
        return repository.getProducts()
    }
}