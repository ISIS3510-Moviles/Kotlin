package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.domain.model.ProductDomain

interface ProductRepository {
    suspend fun getProducts(): List<ProductDTO>
    suspend fun getProductById(id: String): ProductDTO
    suspend fun getProductsByRestaurant(id: String): List<ProductDTO> // Podría devolver ProductDomain para consistencia con UseCases
    suspend fun searchProducts(query: String): List<ProductDTO>

    // Nuevos métodos para CRUD de productos
    suspend fun createProduct(product: CreateProductDTO): ProductDomain // Devuelve ProductDomain
    suspend fun updateProduct(productId: String, productUpdate: UpdateProductDTO): ProductDomain // Devuelve ProductDomain
    suspend fun deleteProduct(productId: String): Boolean // Devuelve true si éxito
}