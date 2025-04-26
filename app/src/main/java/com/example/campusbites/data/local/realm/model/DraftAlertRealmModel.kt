package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class DraftAlertRealmModel : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var message: String = ""
    var restaurantId: String = ""
    var restaurantName: String = ""
    var createdAt: Long = System.currentTimeMillis()
}