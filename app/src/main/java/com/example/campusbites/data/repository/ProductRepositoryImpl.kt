package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.ProductDomain
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

}