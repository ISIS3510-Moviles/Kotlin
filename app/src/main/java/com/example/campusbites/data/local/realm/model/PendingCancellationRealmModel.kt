package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

/**
 * Modelo Realm para almacenar cancelaciones de reserva pendientes
 * cuando no hay conexión.
 */
class PendingCancellationRealmModel : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var reservationId: String = ""
    var createdAt: Long = System.currentTimeMillis()
}