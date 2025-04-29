package com.example.campusbites.data.local.realm

import com.example.campusbites.data.local.realm.model.PendingCancellationRealmModel
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingCancellationLocalDataSource @Inject constructor(
    private val realmConfig: RealmConfig
) {
    private val realm: Realm get() = realmConfig.realm

    /** Agrega una cancelación pendiente */
    suspend fun add(reservationId: String) {
        realm.write {
            copyToRealm(
                PendingCancellationRealmModel().apply {
                    this.reservationId = reservationId
                }
            )
        }
    }

    /** Devuelve todas las cancelaciones pendientes */
    suspend fun getAll(): List<PendingCancellationRealmModel> =
        realm.query<PendingCancellationRealmModel>().find()

    /** Elimina una cancelación ya procesada */
    suspend fun remove(item: PendingCancellationRealmModel) {
        realm.write {
            // Query for the specific object by its primary key (item.id)
            // 'id' is assumed to be the primary key field name in PendingCancellationRealmModel
            // '$0' is a placeholder for the item.id value
            val objectToDelete = query<PendingCancellationRealmModel>("id == $0", item.id)
                .first() // Get the first result (there should be only one with a primary key)
                .find()  // Execute the query and find the object

            // If the object is found (it's a managed object within this transaction)
            if (objectToDelete != null) {
                // Delete the managed object using the delete method available in the transaction
                delete(objectToDelete)
            }
        }
    }
}