package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.domain.model.ProductDomain

interface ProductRepository {
    suspend fun getProducts(): List<ProductDomain> // Cambiado a ProductDomain
    suspend fun getProductById(id: String): ProductDomain? // Cambiado a ProductDomain?
    suspend fun getProductsByRestaurant(id: String): List<ProductDomain> // Cambiado a ProductDomain
    suspend fun searchProducts(query: String): List<ProductDomain> // Cambiado a ProductDomain

    suspend fun createProduct(product: CreateProductDTO): ProductDomain
    suspend fun updateProduct(productId: String, productUpdate: UpdateProductDTO): ProductDomain
    suspend fun deleteProduct(productId: String): Boolean
}