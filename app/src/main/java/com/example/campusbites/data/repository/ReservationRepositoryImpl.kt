package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.CreateReservationDTO
import com.example.campusbites.data.dto.ReservationDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.LocalReservationRepository
import com.example.campusbites.domain.repository.ReservationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Singleton
class ReservationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val localReservationRepository: LocalReservationRepository,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ReservationRepository {

    private val TAG = "ReservationRepo"

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    override fun getReservationsForUser(userId: String): Flow<List<ReservationDomain>> {
        val localDataFlow = localReservationRepository.getReservationsForUser(userId)

        repositoryScope.launch {
            try {
                val remoteReservations = apiService.getReservationsForUser(userId).map { it.toDomain() }
                localReservationRepository.saveReservations(remoteReservations)
            } catch (e: Exception) {
            }
        }

        return localDataFlow
    }

    override suspend fun getReservationById(id: String): ReservationDomain? {
        val localReservation = localReservationRepository.getReservationById(id)
        if (localReservation != null) {
            return localReservation
        }

        return try {
            val remoteReservation = apiService.getReservationById(id).toDomain()
            localReservationRepository.saveReservation(remoteReservation)
            remoteReservation
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createReservation(reservation: ReservationDomain): ReservationDomain {
        val createReservationDTO = CreateReservationDTO(
            user_id = reservation.userId,
            restaurant_id = reservation.restaurantId,
            date = reservation.datetime,
            time = reservation.time,
            numberComensals = reservation.numberCommensals,
            isCompleted = false,
        )
        val createdReservationDTO = apiService.createReservation(createReservationDTO)
        val createdReservationDomain = createdReservationDTO.toDomain()
        localReservationRepository.saveReservation(createdReservationDomain)
        return createdReservationDomain
    }

    override suspend fun cancelReservation(id: String): ReservationDomain {
        val cancelledReservationDTO = apiService.cancelReservation(id)
        val cancelledReservationDomain = cancelledReservationDTO.toDomain()
        localReservationRepository.saveReservation(cancelledReservationDomain)
        return cancelledReservationDomain
    }

    override suspend fun markReservationAsCompleted(id: String): ReservationDomain {
        val completedReservationDTO = apiService.markReservationAsCompleted(id) // Llama al nuevo endpoint
        val completedReservationDomain = completedReservationDTO.toDomain()
        localReservationRepository.saveReservation(completedReservationDomain) // Actualiza el caché local
        return completedReservationDomain
    }

    override fun getReservationsByRestaurantId(restaurantId: String): Flow<List<ReservationDomain>> { // Cambiado id a restaurantId
        val localDataFlow = localReservationRepository.getReservationsByRestaurantId(restaurantId)


        externalScope.launch {
            try {
                Log.d(TAG, "Network fetch: Attempting for restaurant: $restaurantId")
                val remoteReservationsDTO = apiService.getReservationsByRestaurantId(restaurantId)
                Log.d(TAG, "Network fetch: Received ${remoteReservationsDTO.size} DTOs for $restaurantId")

                val remoteReservationsDomain = remoteReservationsDTO.map { it.toDomain() } // Usa tu DTO.toDomain()
                Log.d(TAG, "Network fetch: Mapped to ${remoteReservationsDomain.size} Domain objects for $restaurantId")

                if (remoteReservationsDomain.isNotEmpty()) {
                    Log.d(TAG, "Network fetch: First reservation to save: ID=${remoteReservationsDomain.first().id}, RestaurantID=${remoteReservationsDomain.first().restaurantId}")
                }

                Log.d(TAG, "Network fetch: Saving to local Room cache for $restaurantId...")
                localReservationRepository.saveReservations(remoteReservationsDomain)
                Log.d(TAG, "Network fetch: Successfully saved to Room cache for $restaurantId.")

            } catch (e: Exception) {
                Log.e(TAG, "Network fetch: Error for $restaurantId: ${e.message}", e)
            }
        }
        return localDataFlow
    }

    fun ReservationDTO.toDomain(): ReservationDomain {
        return ReservationDomain(
            id = this.id,
            userId = this.user_id,
            restaurantId = this.restaurant_id,
            datetime = this.date,
            time = this.time,
            numberCommensals = this.numberComensals,
            isCompleted = this.isCompleted,
            hasBeenCancelled = this.hasBeenCancelled
        )
    }
}