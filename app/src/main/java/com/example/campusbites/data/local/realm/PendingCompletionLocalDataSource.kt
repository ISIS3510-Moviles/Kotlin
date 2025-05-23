package com.example.campusbites.data.local.realm

import com.example.campusbites.data.local.realm.model.PendingCompletionRealmModel
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import org.mongodb.kbson.ObjectId // Asegúrate de que esta importación sea correcta
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingCompletionLocalDataSource @Inject constructor(
    private val realmConfig: RealmConfig
) {
    private val realm: Realm get() = realmConfig.realm

    suspend fun add(reservationId: String) {
        realm.write {
            copyToRealm(
                PendingCompletionRealmModel().apply {
                    this.reservationId = reservationId
                    // createdAt se establece por defecto
                }
            )
        }
    }

    suspend fun getAll(): List<PendingCompletionRealmModel> {
        return realm.query<PendingCompletionRealmModel>().find().toList()
    }

    suspend fun remove(item: PendingCompletionRealmModel) {
        realm.write {
            // Es importante encontrar la instancia más reciente del objeto en la transacción actual antes de eliminar
            findLatest(item)?.also { delete(it) }
        }
    }

    // Opcional: si necesitas eliminar por ObjectId directamente
    suspend fun removeByObjectId(objectId: ObjectId) {
        realm.write {
            val actionToDelete = query<PendingCompletionRealmModel>("id == $0", objectId).first().find()
            actionToDelete?.let { delete(it) }
        }
    }
}