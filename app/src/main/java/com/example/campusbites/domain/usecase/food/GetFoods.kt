package com.example.campusbites.domain.usecase.food

import com.example.campusbites.domain.model.Product
import com.example.campusbites.domain.repository.ProductRepository
import jakarta.inject.Inject

class GetFoods @Inject constructor(
    private val repository: ProductRepository
){
    suspend operator fun invoke(): List<Product> {
        return repository.getProducts()
    }
}