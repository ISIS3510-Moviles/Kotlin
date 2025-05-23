package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PendingCompletionRealmModel : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId() // ID local de la acción pendiente
    var reservationId: String = "" // ID de la reserva a marcar como completada
    var createdAt: Long = System.currentTimeMillis() // Opcional: para seguimiento o TTL
}