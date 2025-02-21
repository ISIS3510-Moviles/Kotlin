package com.example.campusbites.di

import com.example.campusbites.data.repository.FakeRestaurantRepositoryImpl
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.restaurant.GetRestaurants
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
    fun provideRestaurantRepository(): RestaurantRepository {
        return FakeRestaurantRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetRestaurantsUseCase(repository: RestaurantRepository): GetRestaurants {
        return GetRestaurants(repository)
    }
}