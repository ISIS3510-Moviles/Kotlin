package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId // Necesitas esta importación para ObjectId

class PendingRestaurantUpdateRealmModel : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var restaurantId: String = ""
    var updatePayloadJson: String = ""
    var timestamp: Long = 0
}