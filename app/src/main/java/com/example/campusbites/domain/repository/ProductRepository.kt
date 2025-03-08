package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): List<Product>
}