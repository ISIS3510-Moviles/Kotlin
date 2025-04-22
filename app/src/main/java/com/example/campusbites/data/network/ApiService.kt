package com.example.campusbites.data.network

import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.data.dto.CommentDTO
import com.example.campusbites.data.dto.CreateAlertDTO
import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.data.dto.DietaryTagDTO
import com.example.campusbites.data.dto.FoodTagDTO
import com.example.campusbites.data.dto.IngredientDTO
import com.example.campusbites.data.dto.InstitutionDTO
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.dto.RecommendationDTO
import com.example.campusbites.data.dto.RecommendationRestaurantDTO
import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateAlertDTO
import com.example.campusbites.data.dto.UserDTO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("restaurant")
    suspend fun getRestaurants(): List<RestaurantDTO>

    @GET("restaurant/{id}")
    suspend fun getRestaurant(@Path("id") id: String): RestaurantDTO

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

    @PATCH("alert/{id}")
    suspend fun updateAlertVotes(@Path("id") id: String,@Body updateAlertDto: UpdateAlertDTO): Boolean

    @POST("alert")
    suspend fun createAlert(@Body createAlertDTO: CreateAlertDTO): Boolean

    @GET("comment")
    suspend fun getComments(): List<CommentDTO>

    @POST("user")
    suspend fun createUser(@Body userDTO: UserDTO): Response<Unit>

    @PATCH("user/{id}")
    suspend fun updateUser(@Path("id") id: String,@Body userDTO: UserDTO): Boolean

    @GET("user")
    suspend fun getUsers(): List<UserDTO>

    @POST("user/recommend")
    suspend fun getRecommendations(@Body recommendationDTO: RecommendationDTO ): Response<List<RecommendationRestaurantDTO>>

    @GET("restaurant")
    suspend fun searchRestaurants(@Query("nameMatch") searchTerm: String?): Response<List<RestaurantDTO>>

    @GET("product")
    suspend fun searchProducts(@Query("nameMatch") searchTerm: String?): Response<List<ProductDTO>>

    @GET("reservation/{id}")
    suspend fun getReservationById(@Path("id") id: String): ReservationDTO

    @POST("reservation")
    suspend fun createReservation(@Body reservationDTO: CreateReservationDTO): ReservationDTO

}