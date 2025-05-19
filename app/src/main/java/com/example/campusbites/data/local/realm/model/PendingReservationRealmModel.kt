package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PendingReservationRealmModel : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId() // ID local de la acción pendiente
    // Campos de la ReservaDomain que necesitamos guardar
    var reservationIdAttempt: String = "" // Podría ser un UUID generado en cliente si el backend no lo asigna
    var restaurantId: String = ""
    var userId: String = ""
    var datetime: String = "" // Formato ISO_INSTANT (UTC)
    var time: String = ""     // Formato HH:mm
    var numberCommensals: Int = 1
    // No necesitamos isCompleted ni hasBeenCancelled para una reserva pendiente de creación
    var createdAt: Long = System.currentTimeMillis()
}