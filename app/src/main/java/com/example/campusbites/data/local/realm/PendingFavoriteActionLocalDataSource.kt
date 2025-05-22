package com.example.campusbites.data.local.realm

import com.example.campusbites.data.local.realm.model.PendingFavoriteActionRealmModel
import io.realm.kotlin.Realm
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import io.realm.kotlin.notifications.ResultsChange
import kotlinx.coroutines.flow.Flow
import org.mongodb.kbson.ObjectId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingFavoriteActionLocalDataSource @Inject constructor(
    private val realmConfig: RealmConfig
) {
    private val realm: Realm get() = realmConfig.realm

    suspend fun addOrUpdateAction(userId: String, productId: String, shouldBeFavorite: Boolean) {
        realm.write {
            // Buscar si ya existe una acción pendiente para este usuario y producto
            val existingAction = query<PendingFavoriteActionRealmModel>("userId == $0 AND productId == $1", userId, productId).first().find()
            if (existingAction != null) {
                // Si existe, actualizarla
                existingAction.shouldBeFavorite = shouldBeFavorite
                existingAction.createdAt = System.currentTimeMillis()
            } else {
                // Si no existe, crear una nueva
                copyToRealm(
                    PendingFavoriteActionRealmModel().apply {
                        this.userId = userId
                        this.productId = productId
                        this.shouldBeFavorite = shouldBeFavorite
                    }
                )
            }
        }
    }

    suspend fun getAllActions(): List<PendingFavoriteActionRealmModel> =
        realm.query<PendingFavoriteActionRealmModel>().find().toList() // toList() para obtener una copia inmutable

    suspend fun removeAction(actionId: ObjectId) {
        realm.write {
            val actionToDelete = query<PendingFavoriteActionRealmModel>("id == $0", actionId).first().find()
            actionToDelete?.let { delete(it) }
        }
    }

    // Opcional: si quieres observar cambios
    fun observePendingActions(): Flow<ResultsChange<PendingFavoriteActionRealmModel>> {
        return realm.query<PendingFavoriteActionRealmModel>().asFlow()
    }
}