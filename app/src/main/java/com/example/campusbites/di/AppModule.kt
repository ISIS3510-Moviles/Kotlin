package com.example.campusbites.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.credentials.CredentialManager
import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.network.CampusBitesApi
import com.example.campusbites.data.local.AppDatabase
import androidx.room.Room
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.repository.AlertRepositoryImpl
import com.example.campusbites.data.repository.CommentRepositoryImpl
import com.example.campusbites.data.repository.DietaryTagRepositoryImpl
import com.example.campusbites.data.repository.DraftAlertRepositoryImpl
import com.example.campusbites.data.repository.FoodTagRepositoryImpl
import com.example.campusbites.data.repository.IngredientRepositoryImpl
import com.example.campusbites.data.repository.InstitutionRepositoryImpl
import com.example.campusbites.data.repository.LocalReservationRepositoryImpl
import com.example.campusbites.data.repository.LocationRepositoryImpl
import com.example.campusbites.data.repository.ProductRepositoryImpl
import com.example.campusbites.data.repository.RecommendationRepositoryImpl
import com.example.campusbites.data.repository.ReservationRepositoryImpl
import com.example.campusbites.data.repository.RestaurantRepositoryImpl
import com.example.campusbites.data.repository.UserRepositoryImpl
import com.example.campusbites.domain.repository.AlertRepository
import com.example.campusbites.domain.repository.CommentRepository
import com.example.campusbites.domain.repository.DietaryTagRepository
import com.example.campusbites.domain.repository.DraftAlertRepository
import com.example.campusbites.domain.repository.FoodTagRepository
import com.example.campusbites.domain.repository.IngredientRepository
import com.example.campusbites.domain.repository.InstitutionRepository
import com.example.campusbites.domain.repository.LocalReservationRepository
import com.example.campusbites.domain.repository.LocationRepository
import com.example.campusbites.domain.repository.ProductRepository
import com.example.campusbites.domain.repository.RecommendationRepository
import com.example.campusbites.domain.repository.ReservationRepository
import com.example.campusbites.domain.repository.RestaurantRepository
import com.example.campusbites.domain.repository.UserRepository
import com.example.campusbites.domain.service.AlertNotificationService
import com.example.campusbites.domain.usecase.comment.CreateCommentUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideConnectivityMonitor(@ApplicationContext context: Context): ConnectivityMonitor {
        return ConnectivityMonitor(context)
    }

    @Provides
    @Singleton
    fun provideAlertNotificationService(@ApplicationContext context: Context): AlertNotificationService {
        return AlertNotificationService(context)
    }

    @Provides
    @Singleton
    fun provideDraftAlertRepository(appDatabase: AppDatabase): DraftAlertRepository {
        return DraftAlertRepositoryImpl(appDatabase.draftAlertDao())
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "campus_bites_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideReservationDao(database: AppDatabase): ReservationDao {
        return database.reservationDao()
    }


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
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
    fun provideCreateCommentUseCase(
        commentRepository: CommentRepository,
        restaurantRepository: RestaurantRepository
    ) = CreateCommentUseCase(commentRepository)

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

        @Binds
        @Singleton
        abstract fun bindLocalReservationRepository(
            localReservationRepositoryImpl: LocalReservationRepositoryImpl
        ): LocalReservationRepository

        @Binds
        @Singleton
        abstract fun bindReservationRepository(
            reservationRepositoryImpl: ReservationRepositoryImpl
        ): ReservationRepository
    }


}