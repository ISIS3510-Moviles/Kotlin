package com.example.campusbites.data.local.realm

import com.example.campusbites.data.local.realm.model.DraftAlertRealmModel
import com.example.campusbites.data.local.realm.model.PendingCancellationRealmModel
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealmConfig @Inject constructor() {
    val realm: Realm by lazy {
        val config = RealmConfiguration.create(
            schema = setOf(
                DraftAlertRealmModel::class,
                PendingCancellationRealmModel::class
            )
        )
        Realm.open(config)
    }
}