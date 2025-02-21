package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class Restaurant(
    val id: String,
    val name: String,
    val description: String,
    @DrawableRes val profilePhoto: Int,
    @DrawableRes val overviewPhoto: Int,
    val rating: Double,
    val distance: Double,
    val comments: List<Comment>,
    val tags: List<String>
)