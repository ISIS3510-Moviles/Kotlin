package com.example.campusbites.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.credentials.CredentialManager
import com.example.campusbites.data.cache.InMemoryAlertCache
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.network.CampusBitesApi
import com.example.campusbites.data.local.AppDatabase
import androidx.room.Room
import com.example.campusbites.data.cache.InMemoryReviewCache
import com.example.campusbites.data.cache.RestaurantLruCache
import com.example.campusbites.data.cache.SearchCache
import com.example.campusbites.data.local.LocalRestaurantDataSource
import com.example.campusbites.data.local.RealmRestaurantDataSource
import com.example.campusbites.data.local.dao.DraftAlertDao
import com.example.campusbites.data.local.dao.PendingProductActionDao
import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.realm.PendingCancellationLocalDataSource
import com.example.campusbites.data.local.realm.PendingCompletionLocalDataSource
import com.example.campusbites.data.local.realm.PendingFavoriteActionLocalDataSource
import com.example.campusbites.data.local.realm.PendingReservationLocalDataSource
import com.example.campusbites.data.local.realm.RealmConfig
import com.example.campusbites.data.mapper.AlertMapper
import com.example.campusbites.data.mapper.InstitutionMapper
import com.example.campusbites.data.mapper.ProductMapper
import com.example.campusbites.data.mapper.ReservationMapper
import com.example.campusbites.data.mapper.RestaurantMapper
import com.example.campusbites.data.mapper.TagMapper
import com.example.campusbites.data.mapper.UserMapper
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.data.preferences.RestaurantPreferencesRepository
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
import com.example.campusbites.domain.repository.AuthRepository
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
import com.example.campusbites.domain.usecase.institution.GetInstitutionByIdUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.reservation.GetReservationByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagByIdUseCase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRestaurantPreferencesRepository(
        @ApplicationContext context: Context
    ): RestaurantPreferencesRepository {
        return RestaurantPreferencesRepository(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // --- Provisión de Mappers ---
    @Provides
    @Singleton
    fun provideReservationMapper(): ReservationMapper {
        return ReservationMapper()
    }

    @Provides
    @Singleton
    fun provideInstitutionMapper(): InstitutionMapper {
        return InstitutionMapper()
    }

    @Provides
    @Singleton
    fun provideTagMapper(): TagMapper {
        return TagMapper()
    }

    @Provides
    @Singleton
    fun provideProductMapper(
        getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
        getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
    ): ProductMapper {
        return ProductMapper(getFoodTagByIdUseCase, getDietaryTagByIdUseCase)
    }

    @Provides
    @Singleton
    fun provideUserMapper(
        getReservationByIdUseCase: GetReservationByIdUseCase,
        getInstitutionByIdUseCase: GetInstitutionByIdUseCase,
        getProductByIdUseCase: GetProductByIdUseCase,
        institutionMapper: InstitutionMapper
    ): UserMapper {
        return UserMapper(getReservationByIdUseCase, getInstitutionByIdUseCase, getProductByIdUseCase, institutionMapper)
    }

    @Provides
    @Singleton
    fun provideRestaurantMapper(
        getFoodTagByIdUseCase: GetFoodTagByIdUseCase,
        getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase
    ): RestaurantMapper {
        return RestaurantMapper(getFoodTagByIdUseCase, getDietaryTagByIdUseCase)
    }

    @Provides
    @Singleton
    fun provideAlertMapper(): AlertMapper {
        return AlertMapper()
    }
    // --- Fin Provisión de Mappers ---


    @Provides
    @Singleton
    fun provideHomeDataRepository(
        @ApplicationContext context: Context,
        json: Json
    ): HomeDataRepository {
        return HomeDataRepository(context, json)
    }

    @Provides
    @Singleton
    fun provideInMemoryAlertCache(applicationScope: CoroutineScope): InMemoryAlertCache {
        return InMemoryAlertCache(applicationScope)
    }

    @Provides
    @Singleton
    fun provideRestaurantLruCache(): RestaurantLruCache {
        return RestaurantLruCache()
    }

    @Provides
    @Singleton
    fun provideSearchCache(): SearchCache {
        return SearchCache()
    }

    @Provides
    @Singleton
    fun provideRealmConfig(): RealmConfig {
        return RealmConfig()
    }

    @Provides
    @Singleton
    fun providePendingFavoriteActionLocalDataSource(realmConfig: RealmConfig): PendingFavoriteActionLocalDataSource {
        return PendingFavoriteActionLocalDataSource(realmConfig)
    }

    @Provides
    @Singleton
    fun providePendingReservationLocalDataSource(realmConfig: RealmConfig): PendingReservationLocalDataSource {
        return PendingReservationLocalDataSource(realmConfig)
    }
    @Provides
    @Singleton
    fun providePendingCompletionLocalDataSource(realmConfig: RealmConfig): PendingCompletionLocalDataSource {
        return PendingCompletionLocalDataSource(realmConfig)
    }

    @Provides
    @Singleton
    fun providePendingCancellationLocalDataSource(realmConfig: RealmConfig): PendingCancellationLocalDataSource {
        return PendingCancellationLocalDataSource(realmConfig)
    }


    @Provides
    @Singleton
    fun provideDraftAlertRepository(draftAlertDao: DraftAlertDao): DraftAlertRepository {
        return DraftAlertRepositoryImpl(draftAlertDao)
    }

    @Provides
    @Singleton
    fun provideDraftAlertDao(appDatabase: AppDatabase): DraftAlertDao {
        return appDatabase.draftAlertDao()
    }

    @Provides
    @Singleton
    fun providePendingProductActionDao(appDatabase: AppDatabase): PendingProductActionDao {
        return appDatabase.pendingProductActionDao()
    }

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
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "campus_bites_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideReservationDao(database: AppDatabase): ReservationDao {
        return database.reservationDao()
    }

    @Provides
    @Singleton
    fun provideInMemoryReviewCache(applicationScope: CoroutineScope): InMemoryReviewCache {
        return InMemoryReviewCache(applicationScope)
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
    fun provideRestaurantRepository(
        apiService: ApiService,
    ): RestaurantRepository {
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
    fun provideProductRepository(
        apiService: ApiService,
        productMapper: ProductMapper,
        pendingProductActionDao: PendingProductActionDao,
        connectivityMonitor: ConnectivityMonitor,
        homeDataRepository: HomeDataRepository, // Inyectar HomeDataRepository
        applicationScope: CoroutineScope
    ): ProductRepository {
        // Pasar homeDataRepository al constructor de ProductRepositoryImpl
        return ProductRepositoryImpl(apiService, productMapper, pendingProductActionDao, connectivityMonitor, homeDataRepository, applicationScope)
    }


    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }


    @Provides
    @Singleton
    fun provideLocationRepository(fusedLocationProviderClient: FusedLocationProviderClient): LocationRepository {
        return LocationRepositoryImpl(fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideLocalRestaurantDataSource(
        realmConfig: RealmConfig,
        gson: Gson
    ): LocalRestaurantDataSource {
        return RealmRestaurantDataSource(realmConfig, gson)
    }

    @Provides
    @Singleton
    fun provideIngredientRepository(apiService: ApiService): IngredientRepository {
        return IngredientRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideCommentRepository(apiService: ApiService, inMemoryReviewCache: InMemoryReviewCache, authRepository: AuthRepository, connectivityManager: ConnectivityManager): CommentRepository{
        return CommentRepositoryImpl(apiService, inMemoryReviewCache, authRepository, connectivityManager)
    }

    @Provides
    @Singleton
    fun provideCreateCommentUseCase(
        commentRepository: CommentRepository
    ) = CreateCommentUseCase(commentRepository)


    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RepositoryBindingModule {
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

    @Module
    @InstallIn(SingletonComponent::class)
    object FirebaseModule {
        @Provides
        @Singleton
        fun provideFirebaseAnalytics(): FirebaseAnalytics {
            return Firebase.analytics
        }
    }
}