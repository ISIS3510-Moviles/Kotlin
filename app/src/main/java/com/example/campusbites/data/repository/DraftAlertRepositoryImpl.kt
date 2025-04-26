package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.local.realm.RealmConfig
import com.example.campusbites.data.local.realm.model.DraftAlertRealmModel
import com.example.campusbites.domain.model.DraftAlert
import com.example.campusbites.domain.repository.DraftAlertRepository
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import javax.inject.Inject

class DraftAlertRepositoryImpl @Inject constructor(
    private val realmConfig: RealmConfig
) : DraftAlertRepository {

    override suspend fun saveDraftAlert(message: String, restaurantId: String, restaurantName: String): String {
        val realm = realmConfig.realm
        val id = ObjectId()

        realm.write {
            val draftAlert = DraftAlertRealmModel().apply {
                this.id = id
                this.message = message
                this.restaurantId = restaurantId
                this.restaurantName = restaurantName
                this.createdAt = System.currentTimeMillis()
            }
            copyToRealm(draftAlert)
        }

        return id.toHexString()
    }

    override suspend fun getLatestDraftAlert(): DraftAlert? {
        val realm = realmConfig.realm
        return realm.query<DraftAlertRealmModel>()
            .sort("createdAt", io.realm.kotlin.query.Sort.DESCENDING)
            .first()
            .find()
            ?.toDomain()
    }

    override fun getAllDraftAlerts(): Flow<List<DraftAlert>> {
        val realm = realmConfig.realm
        return realm.query<DraftAlertRealmModel>()
            .sort("createdAt", io.realm.kotlin.query.Sort.DESCENDING)
            .asFlow()
            .map { results ->
                results.list.map { it.toDomain() }
            }
    }

    override suspend fun deleteDraftAlert(id: String) {
        val realm = realmConfig.realm
        try {
            Log.d("DraftAlertRepository", "Attempting to delete draft with ID: $id")

            realm.write {
                try {
                    val objectId = ObjectId.invoke(id)
                    Log.d("DraftAlertRepository", "Created ObjectId successfully: $objectId")

                    val alertToDelete = query<DraftAlertRealmModel>("id == $0", objectId).first()

                    if (alertToDelete != null) {
                        delete(alertToDelete)
                        Log.d("DraftAlertRepository", "Draft deleted successfully")
                        return@write
                    } else {
                        Log.w("DraftAlertRepository", "No draft found with ObjectId: $objectId")
                    }
                } catch (e: Exception) {
                    Log.e("DraftAlertRepository", "Failed to convert ID to ObjectId: $id", e)
                }

                // Método alternativo: buscar por todos los drafts y eliminar por comparación de string
                Log.d("DraftAlertRepository", "Trying alternative approach - fetching all drafts")
                val allDrafts = query<DraftAlertRealmModel>().find()
                Log.d("DraftAlertRepository", "Found ${allDrafts.size} total drafts")

                val draftToDelete = allDrafts.firstOrNull {
                    it.id.toHexString() == id
                }

                if (draftToDelete != null) {
                    Log.d("DraftAlertRepository", "Found draft by hex comparison: ${draftToDelete.id}")
                    delete(draftToDelete)
                    Log.d("DraftAlertRepository", "Draft deleted successfully")
                } else {
                    Log.w("DraftAlertRepository", "No matching draft found by any method for ID: $id")
                    allDrafts.forEach {
                        Log.d("DraftAlertRepository", "Available draft: ${it.id}, hex: ${it.id.toHexString()}")
                    }
                    throw IllegalStateException("Draft with ID $id not found")
                }
            }
        } catch (e: Exception) {
            Log.e("DraftAlertRepository", "Error deleting draft alert: $id", e)
            throw e
        }
    }

    override suspend fun hasDraftAlerts(): Boolean {
        val realm = realmConfig.realm
        return realm.query<DraftAlertRealmModel>().count().find() > 0
    }

    private fun DraftAlertRealmModel.toDomain(): DraftAlert {
        return DraftAlert(
            // IMPORTANTE: Usar toHexString() para mantener consistencia
            id = this.id.toHexString(),
            message = this.message,
            restaurantId = this.restaurantId,
            restaurantName = this.restaurantName,
            createdAt = this.createdAt
        )
    }
}