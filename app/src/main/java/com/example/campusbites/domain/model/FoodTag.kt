package com.example.campusbites.domain.model

data class FoodTag(
    override val id: Int,
    override val name: String,
    override val description: String,
    override val restaurants: List<Restaurant>  = emptyList(),
    override val products: List<Product>  = emptyList(),
    val icon: Photo
) : Tag(id, name, description, restaurants, products)
