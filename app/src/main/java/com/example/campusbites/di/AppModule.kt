package com.example.campusbites.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.network.CampusBitesApi
import com.example.campusbites.data.repository.AlertRepositoryImpl
import com.example.campusbites.data.repository.CommentRepositoryImpl
import com.example.campusbites.data.repository.DietaryTagRepositoryImpl
import com.example.campusbites.data.repository.FoodTagRepositoryImpl
import com.example.campusbites.data.repository.IngredientRepositoryImpl
import com.example.campusbites.data.repository.InstitutionRepositoryImpl
import com.example.campusbites.data.repository.LocationRepositoryImpl
import com.example.campusbites.data.repository.ProductRepositoryImpl
import com.example.campusbites.data.repository.RecommendationRepositoryImpl
import com.example.campusbites.data.repository.ReservationRepositoryImpl
import com.example.campusbites.data.repository.RestaurantRepositoryImpl
import com.example.campusbites.data.repository.UserRepositoryImpl
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.repository.CommentRepository
import com.example.campusbites.domain.repository.DietaryTagRepository
import com.example.campusbites.domain.repository.FoodTagRepository
import com.example.campusbites.domain.repository.IngredientRepository
import com.example.campusbites.domain.repository.InstitutionRepository
import com.example.campusbites.domain.repository.LocationRepository
import com.example.campusbites.domain.repository.ProductRepository
import com.example.campusbites.domain.repository.RecommendationRepository
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.repository.UserRepository
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager {
        return CredentialManager.create(context)
    }


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

    @Provides
    @Singleton
    fun provideRecommendationRepository(apiService: ApiService): RecommendationRepository {
        return RecommendationRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideFoodTagRepository(apiService: ApiService): FoodTagRepository {
        return FoodTagRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideDietaryTagRepository(apiService: ApiService): DietaryTagRepository {
        return DietaryTagRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideUserRepository(apiService: ApiService): UserRepository {
        return UserRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideInstitutionRepository(apiService: ApiService): InstitutionRepository {
        return InstitutionRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideProductRepository(apiService: ApiService): ProductRepository {
        return ProductRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(fusedLocationProviderClient: FusedLocationProviderClient): LocationRepository {
        return LocationRepositoryImpl(fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideIngredientRepository(apiService: ApiService): IngredientRepository {
        return IngredientRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(apiService: ApiService): CommentRepository{
        return CommentRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }


    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RepositoryModule {
        @Binds
        @Singleton
        abstract fun bindAlertRepository(
            alertRepositoryImpl: AlertRepositoryImpl
        ): AlertRepository
    }

    @Provides
    @Singleton
    fun provideInMemoryAlertCache(): InMemoryAlertCache {
        return InMemoryAlertCache()
    }

    @Provides
    @Singleton
    fun provideReservationsRepository(apiService: ApiService): ReservationRepository {
        return ReservationRepositoryImpl(apiService)
    }

}