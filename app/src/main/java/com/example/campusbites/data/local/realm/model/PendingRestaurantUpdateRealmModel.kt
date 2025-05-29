package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId // Necesitas esta importación para ObjectId

/**
 * Modelo Realm para almacenar actualizaciones de restaurantes pendientes de sincronización.
 */
class PendingRestaurantUpdateRealmModel : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId() // Clave primaria única para cada actualización pendiente
    var restaurantId: String = "" // El ID del restaurante que se está actualizando
    var updatePayloadJson: String = "" // El DTO de actualización del restaurante serializado a JSON
    var timestamp: Long = 0 // Marca de tiempo de cuándo se guardó la actualización localmente (Epoch Millis)
}