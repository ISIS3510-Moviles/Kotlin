package com.example.campusbites.data.repository

import com.example.campusbites.data.local.dao.ReservationDao
import com.example.campusbites.data.local.entity.ReservationEntity
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.repository.LocalReservationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalReservationRepositoryImpl @Inject constructor(
    private val reservationDao: ReservationDao
) : LocalReservationRepository {

    override suspend fun saveReservations(reservations: List<ReservationDomain>) {
        val entities = reservations.map { it.toEntity() }
        reservationDao.insertReservations(entities)
    }

    override suspend fun saveReservation(reservation: ReservationDomain) {
        reservationDao.insertReservation(reservation.toEntity())
    }

    override fun getReservationsForUser(userId: String): Flow<List<ReservationDomain>> {
        return reservationDao.getReservationsForUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getReservationById(reservationId: String): ReservationDomain? {
        return reservationDao.getReservationById(reservationId)?.toDomain()
    }

    override suspend fun deleteReservationById(reservationId: String) {
        reservationDao.deleteReservationById(reservationId)
    }

    override suspend fun deleteAllReservationsForUser(userId: String) {
        reservationDao.deleteAllReservationsForUser(userId)
    }
}


fun ReservationDomain.toEntity(): ReservationEntity {
    return ReservationEntity(
        id = this.id,
        userId = this.userId,
        restaurantId = this.restaurantId,
        datetime = this.datetime,
        time = this.time,
        numberCommensals = this.numberCommensals,
        isCompleted = this.isCompleted,
        hasBeenCancelled = this.hasBeenCancelled
    )
}

fun ReservationEntity.toDomain(): ReservationDomain {
    return ReservationDomain(
        id = this.id,
        userId = this.userId,
        restaurantId = this.restaurantId,
        datetime = this.datetime,
        time = this.time,
        numberCommensals = this.numberCommensals,
        isCompleted = this.isCompleted,
        hasBeenCancelled = this.hasBeenCancelled!!
    )
}