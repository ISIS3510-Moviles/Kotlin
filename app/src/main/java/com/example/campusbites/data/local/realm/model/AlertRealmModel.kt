// package com.example.campusbites.data.local.realm.model (o donde corresponda)
package com.example.campusbites.data.local.realm.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

open class AlertRealmModel : RealmObject {
    @PrimaryKey
    var id: String = ""
    var datetime: String = "" // Store as ISO String
    var icon: String = ""
    var message: String = ""
    var votes: Int = 0

    // Denormalized publisher info for easier display from cache
    var publisherId: String = ""
    var publisherName: String = "" // Add this for simpler display

    // Denormalized restaurant info
    var restaurantId: String = ""
    var restaurantName: String = "" // Add this for simpler display

    constructor(
        id: String,
        datetime: String,
        icon: String,
        message: String,
        votes: Int,
        publisherId: String,
        publisherName: String,
        restaurantId: String,
        restaurantName: String
    ) : this() {
        this.id = id
        this.datetime = datetime
        this.icon = icon
        this.message = message
        this.votes = votes
        this.publisherId = publisherId
        this.publisherName = publisherName
        this.restaurantId = restaurantId
        this.restaurantName = restaurantName
    }

    constructor() // Required public no-arg constructor
}