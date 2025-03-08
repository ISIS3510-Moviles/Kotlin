package com.example.campusbites.domain.model

data class Comment(
    val id: Int,
    val datetime: String,
    val message: String,
    val rating: Int,
    val likes: Int,
    val photos: List<Photo>?,
    val isVisible: Boolean,
    val author: User,
    val responses: List<Comment> = emptyList(),
    val responseTo: Comment?,
    val reports: List<Report> = emptyList(),
    val product: Product?,
    val restaurant: Restaurant,
)