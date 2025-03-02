package com.example.campusbites.domain.model

import androidx.annotation.DrawableRes

data class Alert (

    val id: String,
    @DrawableRes val imageRes: Int,
    val title: String,
    val message: String
)