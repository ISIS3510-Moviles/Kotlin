package com.example.campusbites.data.repository

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
    private val localReservationRepository: LocalReservationRepository
) : ReservationRepository {

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

    override suspend fun createReservation(reservation: CreateReservationDTO): ReservationDomain {
        val createReservationDTO = CreateReservationDTO(
            user_id = reservation.user_id,
            restaurant_id = reservation.restaurant_id,
            date = reservation.date,
            time = reservation.time,
            numberComensals = reservation.numberComensals,
            isCompleted = reservation.isCompleted,
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