package com.example.campusbites.data.local.realm

import com.example.campusbites.data.local.realm.model.PendingReservationRealmModel
import com.example.campusbites.domain.model.ReservationDomain
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import org.mongodb.kbson.ObjectId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingReservationLocalDataSource @Inject constructor(
    private val realmConfig: RealmConfig
) {
    private val realm: Realm get() = realmConfig.realm

    suspend fun addPendingReservation(reservation: ReservationDomain) {
        realm.write {
            copyToRealm(
                PendingReservationRealmModel().apply {
                    // reservation.id podría estar vacío si es una nueva reserva.
                    // Generamos un ID temporal si es necesario para la cola, pero el backend asignará el final.
                    this.reservationIdAttempt = if (reservation.id.isBlank()) UUID.randomUUID().toString() else reservation.id
                    this.restaurantId = reservation.restaurantId
                    this.userId = reservation.userId
                    this.datetime = reservation.datetime
                    this.time = reservation.time
                    this.numberCommensals = reservation.numberCommensals
                }
            )
        }
    }

    suspend fun getAllPendingReservations(): List<PendingReservationRealmModel> =
        realm.query<PendingReservationRealmModel>().find().toList()

    suspend fun removePendingReservation(pendingId: ObjectId) {
        realm.write {
            val actionToDelete = query<PendingReservationRealmModel>("id == $0", pendingId).first().find()
            actionToDelete?.let { delete(it) }
        }
    }
}