package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class PendingFavoriteActionRealmModel : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var userId: String = ""
    var productId: String = ""
    var shouldBeFavorite: Boolean = true // true para agregar, false para quitar
    var createdAt: Long = System.currentTimeMillis()
}