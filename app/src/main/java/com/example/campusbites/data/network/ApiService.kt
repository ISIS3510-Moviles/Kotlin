package com.example.campusbites.data.network

import com.example.campusbites.domain.model.Restaurant
import retrofit2.http.GET

interface ApiService {
    @GET("restaurant")
    suspend fun getRestaurants(): List<Restaurant>
}