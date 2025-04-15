package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.domain.model.ProductDomain

interface ProductRepository {
    suspend fun getProducts(): List<ProductDTO>
    suspend fun getProductById(id: String): ProductDTO
    suspend fun getProductsByRestaurant(id: String): List<ProductDTO>
    suspend fun searchProducts(query: String): List<ProductDTO>
}