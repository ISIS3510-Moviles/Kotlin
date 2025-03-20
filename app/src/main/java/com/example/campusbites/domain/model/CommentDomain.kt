package com.example.campusbites.domain.model

data class CommentDomain(
    val id: String,
    val datetime: String?,
    val message: String,
    val rating: Int,
    val likes: Int?,
    val photo: List<String>?,
    val isVisible: Boolean,
    val author: UserDomain,
    val responses: List<CommentDomain>? = emptyList(),
    val responseTo: CommentDomain?,
    val reports: List<ReportDomain>? = emptyList(),
    val productDomain: ProductDomain?,
    val restaurantDomain: RestaurantDomain?,
)