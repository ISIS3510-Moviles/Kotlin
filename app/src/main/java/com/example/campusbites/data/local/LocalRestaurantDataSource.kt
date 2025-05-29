package com.example.campusbites.data.local

import com.example.campusbites.data.local.realm.RealmConfig
import com.example.campusbites.data.local.realm.model.PendingRestaurantUpdateRealmModel
import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.google.gson.Gson
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import org.mongodb.kbson.ObjectId // Importar ObjectId

interface LocalRestaurantDataSource {
    suspend fun savePendingUpdate(restaurantId: String, updateDTO: UpdateRestaurantDTO): String
    fun getAllPendingUpdates(): Flow<List<PendingRestaurantUpdateRealmModel>>
    suspend fun deletePendingUpdate(updateId: String)
    fun deserializeUpdatePayload(json: String): UpdateRestaurantDTO
}

@Singleton
class RealmRestaurantDataSource @Inject constructor(
    private val realmConfig: RealmConfig,
    private val gson: Gson
) : LocalRestaurantDataSource {

    override suspend fun savePendingUpdate(restaurantId: String, updateDTO: UpdateRestaurantDTO): String = withContext(Dispatchers.IO) {
        var newObjectId: String = ""
        realmConfig.realm.write { // Acceso a realm.write() dentro de Dispatchers.IO
            val realmModel = PendingRestaurantUpdateRealmModel().apply {
                this.restaurantId = restaurantId
                this.updatePayloadJson = gson.toJson(updateDTO)
                this.timestamp = Instant.now().toEpochMilli()
            }
            copyToRealm(realmModel, UpdatePolicy.ALL)
            newObjectId = realmModel._id.toHexString()
        }
        newObjectId
    }

    override fun getAllPendingUpdates(): Flow<List<PendingRestaurantUpdateRealmModel>> {
        // Realm Flow ya maneja su propio hilo, pero flowOn(Dispatchers.IO) asegura que el mapeo también sea en IO
        return realmConfig.realm.query<PendingRestaurantUpdateRealmModel>().find().asFlow()
            .map { resultsChange ->
                resultsChange.list
            }.flowOn(Dispatchers.IO) // Asegura que la recolección y el mapeo se hagan en IO
    }

    override suspend fun deletePendingUpdate(updateId: String): Unit = withContext(Dispatchers.IO) {
        realmConfig.realm.write { // Acceso a realm.write() dentro de Dispatchers.IO
            val entityToDelete = query<PendingRestaurantUpdateRealmModel>("_id == $0", ObjectId(updateId)).first().find()
            entityToDelete?.let { delete(it) }
        }
    }

    override fun deserializeUpdatePayload(json: String): UpdateRestaurantDTO {
        return gson.fromJson(json, UpdateRestaurantDTO::class.java)
    }
}