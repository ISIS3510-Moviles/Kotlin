package com.example.campusbites.domain.model

data class DietaryTag(
    override val id: Int,
    override val name: String,
    override val description: String,
    override val restaurants: List<Restaurant>  = emptyList(),
    override val products: List<Product>  = emptyList(),
    val appearance: String
) : Tag(id, name, description, restaurants, products)
