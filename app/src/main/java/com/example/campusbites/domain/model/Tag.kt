package com.example.campusbites.domain.model

sealed class Tag(
    open val id: Int,
    open val name: String,
    open val description: String,
    open val restaurants: List<Restaurant>,
    open val products: List<Product>
)
