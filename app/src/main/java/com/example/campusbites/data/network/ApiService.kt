package com.example.campusbites.data.network

import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.data.dto.DietaryTagDTO
import com.example.campusbites.data.dto.FoodTagDTO
import com.example.campusbites.data.dto.IngredientDTO
import com.example.campusbites.data.dto.InstitutionDTO
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("restaurant")
    suspend fun getRestaurants(): List<RestaurantDTO>

    @GET("food-tag")
    suspend fun getFoodTags(): List<FoodTagDTO>

    @GET("food-tag/{id}")
    suspend fun getFoodTagById(@Path("id") id: String): FoodTagDTO

    @GET("dietary-tag")
    suspend fun getDietaryTags(): List<DietaryTagDTO>

    @GET("dietary-tag/{id}")
    suspend fun getDietaryTagById(@Path("id") id: String): DietaryTagDTO

    @GET("user/{id}")
    suspend fun getUserById(@Path("id") id: String): UserDTO

    @GET("product")
    suspend fun getProducts(): List<ProductDTO>

    @GET("product/{id}")
    suspend fun getProductById(@Path("id") id: String): ProductDTO

    @GET("institution/{id}")
    suspend fun getInstitutionById(@Path("id") id: String): InstitutionDTO

    @GET("ingredient")
    suspend fun getIngredients(): List<IngredientDTO>

    @GET("alert")
    suspend fun getAlerts(): List<AlertDTO>

}