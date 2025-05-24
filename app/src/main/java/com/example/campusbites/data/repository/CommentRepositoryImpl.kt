package com.example.campusbites.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.campusbites.data.cache.InMemoryReviewCache
import com.example.campusbites.data.dto.CommentDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex // Importa Mutex
import kotlinx.coroutines.sync.withLock // Importa withLock

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val inMemoryReviewCache: InMemoryReviewCache,
    private val authRepository: AuthRepository,
    private val connectivityManager: ConnectivityManager
) : CommentRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _pendingReviews = MutableStateFlow<List<CommentDomain>>(emptyList())
    val pendingReviews: Flow<List<CommentDomain>> = _pendingReviews.asStateFlow()

    // Mutex para controlar el acceso a la sincronización
    private val syncMutex = Mutex()

    init {
        repositoryScope.launch {
            connectivityManager.isOnline()
                .filter { it }
                .collect {
                    syncPendingReviews()
                }
        }
    }

    override fun getComments(restaurantId: String): Flow<List<CommentDomain>> {
        val cachedReviewsFlow = inMemoryReviewCache.reviews
        Log.d("CommentRepositoryImpl", "Cached reviews value: ${cachedReviewsFlow.value}")

        repositoryScope.launch {
            syncMutex.withLock {
                try {

                    val remoteComments = apiService.getComments().map { it.toDomain(it) }
                    Log.d("CommentRepositoryImpl", "Remote comments: $remoteComments")

                    inMemoryReviewCache.updateReviews(remoteComments)
                } catch (e: Exception) {
                    Log.e("CommentRepositoryImpl", "Error fetching comments: ${e.message}")
                }
            }
        }

        return combine(cachedReviewsFlow, _pendingReviews) { cached, pending ->
            val combinedList = cached.toMutableList()
            pending.forEach { pendingReview ->
                if (combinedList.none { it.id == pendingReview.id }) {
                    combinedList.add(pendingReview)
                }
            }
            combinedList
        }.map { reviews ->
            reviews.filter { it.restaurantDomain?.id == restaurantId }
        }
    }

    override fun getAllComments(): Flow<List<CommentDomain>> {
        val cachedReviewsFlow = inMemoryReviewCache.reviews

        repositoryScope.launch {
            syncMutex.withLock {
                try {
                    val remoteComments = apiService.getComments().map { it.toDomain(it) }
                    Log.d("CommentRepositoryImpl", "Remote comments: $remoteComments")
                    inMemoryReviewCache.updateReviews(remoteComments)
                } catch (e: Exception) {
                    Log.e("CommentRepositoryImpl", "Error fetching all comments: ${e.message}")
                }
            }
        }

        return combine(cachedReviewsFlow, _pendingReviews) { cached, pending ->
            val combinedList = cached.toMutableList()
            pending.forEach { pendingReview ->
                if (combinedList.none { it.id == pendingReview.id }) {
                    combinedList.add(pendingReview)
                }
            }
            combinedList
        }
    }

    override suspend fun createComment(comment: CommentDomain): CommentDomain {
        val currentReviews = inMemoryReviewCache.getReviews().toMutableList()
        currentReviews.add(comment)
        inMemoryReviewCache.updateReviews(currentReviews)
        Log.d("CommentRepositoryImpl", "Added review to cache immediately: ${comment.id}")

        repositoryScope.launch {
            syncMutex.withLock {
                try {
                    val createdCommentDTO = apiService.createComment(comment.toDto())
                    val createdCommentDomain = createdCommentDTO.toDomain(createdCommentDTO)

                    val reviewsAfterRemote = inMemoryReviewCache.getReviews().toMutableList()
                    val index = reviewsAfterRemote.indexOfFirst { it.id == comment.id }
                    if (index != -1) {
                        reviewsAfterRemote[index] = createdCommentDomain
                    } else {
                        reviewsAfterRemote.add(createdCommentDomain)
                    }
                    inMemoryReviewCache.updateReviews(reviewsAfterRemote)
                    Log.d("CommentRepositoryImpl", "Updated cache with remote review: ${createdCommentDomain.id}")

                    _pendingReviews.update { currentList ->
                        currentList.filter { it.id != comment.id }
                    }

                } catch (e: Exception) {
                    Log.e("CommentRepositoryImpl", "Failed to create comment remotely: ${e.message}")
                    addPendingReview(comment)
                }
            }
        }

        return comment
    }

    private fun addPendingReview(comment: CommentDomain) {
        _pendingReviews.update { currentList ->
            currentList + comment
        }
        Log.d("CommentRepositoryImpl", "Added review to pending queue: ${comment.id}. Queue size: ${_pendingReviews.value.size}")
    }

    private fun syncPendingReviews() {
        repositoryScope.launch {
            syncMutex.withLock {
                val pending = _pendingReviews.value.toList()
                if (pending.isNotEmpty()) {
                    Log.d("CommentRepositoryImpl", "Attempting to sync ${pending.size} pending reviews.")
                    val successfullySynced = mutableListOf<CommentDomain>()
                    for (review in pending) {
                        try {
                            val createdCommentDTO = apiService.createComment(review.toDto())
                            val createdCommentDomain = createdCommentDTO.toDomain(createdCommentDTO)
                            successfullySynced.add(review)

                            val currentReviews = inMemoryReviewCache.getReviews().toMutableList()
                            val index = currentReviews.indexOfFirst { it.id == review.id }
                            if (index != -1) {
                                currentReviews[index] = createdCommentDomain
                            } else {
                                currentReviews.add(createdCommentDomain)
                            }
                            inMemoryReviewCache.updateReviews(currentReviews)

                            Log.d("CommentRepositoryImpl", "Successfully synced pending review: ${review.id}")
                        } catch (e: Exception) {
                            Log.e("CommentRepositoryImpl", "Failed to sync pending review: ${review.id}. Error: ${e.message}")
                        }
                    }

                    _pendingReviews.update { currentList ->
                        currentList.filter { it !in successfullySynced }
                    }
                    Log.d("CommentRepositoryImpl", "Finished syncing pending reviews. Remaining in queue: ${_pendingReviews.value.size}")
                }
            }
        }
    }

    suspend fun CommentDTO.toDomain(dto: CommentDTO): CommentDomain {
        val author = authRepository.currentUser.firstOrNull() ?: UserDomain(
            id = dto.authorId,
            name = "Unknown User",
            email = "",
            phone = "",
            role = "",
            isPremium = false,
            badgesIds = emptyList(),
            schedulesIds = emptyList(),
            reservationsDomain = emptyList(),
            institution = null,
            dietaryPreferencesTagIds = emptyList(),
            commentsIds = emptyList(),
            visitsIds = emptyList(),
            suscribedRestaurantIds = emptyList(),
            publishedAlertsIds = emptyList(),
            savedProducts = emptyList(),
            vendorRestaurantId = null
        )

        val restaurant = try {
            dto.restaurantId?.let {
                withContext(Dispatchers.IO) { apiService.getRestaurant(it).toDomain() }
            }
        } catch (e: Exception) {
            Log.e("CommentRepositoryImpl", "Error fetching restaurant for comment: ${e.message}")
            null
        }

        return CommentDomain(
            id = this.id,
            datetime = this.datetime,
            message = this.message,
            rating = this.rating,
            likes = this.likes,
            photo = this.photos,
            isVisible = this.isVisible,
            author = author,
            responses = emptyList(),
            responseTo = null,
            reports = emptyList(),
            productDomain = null,
            restaurantDomain = restaurant
        )
    }

    fun CommentDomain.toDto(): CommentDTO {
        return CommentDTO(
            id = this.id,
            message = this.message,
            rating = this.rating,
            likes = this.likes,
            isVisible = this.isVisible,
            photos = this.photo,
            responseToId = this.responseTo?.id,
            restaurantId = this.restaurantDomain?.id,
            productId = this.productDomain?.id,
            reportsIds = this.reports?.map { it.id.toString() },
            responsesIds = this.responses?.map { it.id },
            authorId = this.author.id,
            datetime = this.datetime
        )
    }

    fun UserDTO.toDomain(): UserDomain {
        return UserDomain(
            id = this.id,
            name = this.name,
            phone = this.phone,
            email = this.email,
            role = this.role,
            isPremium = this.isPremium,
            badgesIds = emptyList(),
            schedulesIds = emptyList(),
            reservationsDomain = emptyList(),
            institution = InstitutionDomain(
                id = this.institutionId,
                name = "Institución desconocida",
                description = "",
                members = emptyList(),
                buildings = emptyList()
            ),
            dietaryPreferencesTagIds = emptyList(),
            commentsIds = emptyList(),
            visitsIds = emptyList(),
            suscribedRestaurantIds = emptyList(),
            publishedAlertsIds = emptyList(),
            savedProducts = emptyList(),
            vendorRestaurantId = null
        )
    }

    fun RestaurantDTO.toDomain(): RestaurantDomain {
        return RestaurantDomain(
            id = this.id,
            name = this.name,
            description = this.description,
            rating = this.rating,
            foodTags = emptyList(),
            dietaryTags = emptyList(),
            photos = this.photos,
            latitude = this.latitude,
            longitude = this.longitude,
            routeIndications = this.routeIndications,
            openingTime = this.openingTime,
            closingTime = this.closingTime,
            opensHolidays = this.opensHolidays,
            opensWeekends = this.opensWeekends,
            isActive = this.isActive,
            address = this.address,
            phone = this.phone,
            email = this.email,
            overviewPhoto = this.overviewPhoto,
            profilePhoto = this.profilePhoto,
            alertsIds = emptyList(),
            reservationsIds = emptyList(),
            suscribersIds = emptyList(),
            visitsIds = emptyList(),
            commentsIds = emptyList(),
            productsIds = emptyList(),
        )
    }
}

fun ConnectivityManager.isOnline(): Flow<Boolean> = callbackFlow {
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            trySend(true)
        }

        override fun onLost(network: android.net.Network) {
            trySend(false)
        }
    }
    registerDefaultNetworkCallback(networkCallback)
    val capabilities = getNetworkCapabilities(activeNetwork)
    val isConnected = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    trySend(isConnected)
    awaitClose { unregisterNetworkCallback(networkCallback) }
}.distinctUntilChanged()