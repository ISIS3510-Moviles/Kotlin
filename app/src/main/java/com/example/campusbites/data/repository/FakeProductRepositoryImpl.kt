package com.example.campusbites.data.repository

import com.example.campusbites.data.TestData
import com.example.campusbites.domain.model.Product
import com.example.campusbites.domain.repository.ProductRepository

class FakeProductRepositoryImpl: ProductRepository {
    override suspend fun getProducts(): List<Product> {
        return emptyList()
    }
}