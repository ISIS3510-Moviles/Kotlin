package com.example.campusbites.data.local.realm

import android.util.Log
import com.example.campusbites.data.local.realm.model.AlertRealmModel
import com.example.campusbites.data.local.realm.model.DraftAlertRealmModel
import com.example.campusbites.data.local.realm.model.PendingCancellationRealmModel
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealmConfig @Inject constructor() {
    companion object {
        private const val REALM_VERSION = 1L
    }

    val realm: Realm by lazy {
        Log.d("RealmConfig", "Initializing Realm...")
        try {
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    DraftAlertRealmModel::class,
                    PendingCancellationRealmModel::class,
                    AlertRealmModel::class
                )
            )
                .schemaVersion(REALM_VERSION)
                .deleteRealmIfMigrationNeeded()
                .build()

            val realmInstance = Realm.open(config)
            Log.d("RealmConfig", "Realm initialized successfully.")
            realmInstance
        } catch (e: Exception) {
            Log.e("RealmConfig", "Error initializing Realm: ${e.message}", e)
            throw IllegalStateException("Failed to initialize Realm. See logs for details.", e)
        }
    }

}