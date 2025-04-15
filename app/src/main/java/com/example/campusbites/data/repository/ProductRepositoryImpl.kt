package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.ProductRepository
import jakarta.inject.Inject


class ProductRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): ProductRepository {

    override suspend fun getProducts(): List<ProductDTO> {
        return apiService.getProducts()
    }

    override suspend fun getProductById(id: String): ProductDTO {
        return apiService.getProductById(id)
    }

    override suspend fun getProductsByRestaurant(id: String): List<ProductDTO> {
        val allProducts = apiService.getProducts()
        val filteredProducts = allProducts.filter { it.restaurant_id == id }
        return filteredProducts
    }

    override suspend fun searchProducts(query: String): List<ProductDTO> {
        return try {

            val response = apiService.searchProducts(query)

            if (response.isSuccessful) {
                val products = response.body() ?: emptyList()
                products
            } else {
                Log.e("ProductRepository", "❌ Error HTTP: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ProductRepository", "💥 Excepción al buscar productos", e)
            emptyList()
        }
    }




}