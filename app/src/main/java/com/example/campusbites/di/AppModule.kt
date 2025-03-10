package com.example.campusbites.di

import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.network.CampusBitesApi
import com.example.campusbites.data.repository.RestaurantRepositoryImpl
import com.example.campusbites.domain.repository.RestaurantRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return CampusBitesApi.retrofitService
    }

    @Provides
    @Singleton
    fun provideRestaurantRepository(apiService: ApiService): RestaurantRepository {
        return RestaurantRepositoryImpl(apiService)
    }


}