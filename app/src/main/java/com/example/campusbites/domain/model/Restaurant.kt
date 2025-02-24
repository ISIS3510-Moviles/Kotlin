package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class Restaurant(
    @DrawableRes val profilePhoto: Int,
    @DrawableRes val overviewPhoto: Int,

    val id: String,
    val name: String,
    val description: String,

    val rating: Double,
    val distance: Double,

    val comments: List<Comment>,
    val tags: List<String>
)