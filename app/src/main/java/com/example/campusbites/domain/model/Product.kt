package com.example.campusbites.domain.model

data class Product(
    val id: String,
    val photo: Photo,
    val name: String,
    val description: String,
    val rating: Int?,
    val price: Int?,
    val isAvailable: Boolean?,
    val comments: List<Comment>? = emptyList(),
    val ingredients: List<Ingredient>? = emptyList(),
    val tags: List<Tag>? = emptyList(),
    val discounts: List<Discount>? = emptyList(),
    val restaurant: Restaurant?
)
