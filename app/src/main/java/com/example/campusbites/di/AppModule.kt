package com.example.campusbites.di

import com.example.campusbites.data.repository.FakeFoodRepositoryImpl
import com.example.campusbites.data.repository.FakeFoodTagRepositoryImpl
import com.example.campusbites.data.repository.FakeRestaurantRepositoryImpl
import com.example.campusbites.domain.repository.FoodRepository
import com.example.campusbites.domain.repository.FoodTagRepository
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.usecase.food.GetFoodTags
import com.example.campusbites.domain.usecase.restaurant.GetRestaurants
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantById
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

    @Provides
    @Singleton
    fun provideFoodTagRepository(): FoodTagRepository {
        return FakeFoodTagRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetFoodTagsUseCase(repository: FoodTagRepository): GetFoodTags {
        return GetFoodTags(repository)
    }

    @Provides
    @Singleton
    fun provideFoodRepository(): FoodRepository {
        return FakeFoodRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideGetRestaurantByIdUseCase(repository: RestaurantRepository): GetRestaurantById {
        return GetRestaurantById(repository)
    }

}