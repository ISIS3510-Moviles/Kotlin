package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class FoodTag (
    val name: String,
    @DrawableRes val icon: Int
)