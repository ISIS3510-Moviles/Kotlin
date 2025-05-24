package com.example.campusbites.data.repository.impl // Cambiado a .impl para consistencia

import android.util.Log
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateRestaurantCommentsDTO
import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.local.LocalRestaurantDataSource // Importar la interfaz del DataSource local
import com.example.campusbites.data.network.ConnectivityMonitor // Importar el monitor de conectividad
import com.example.campusbites.domain.repository.RestaurantRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Importar withContext para asegurar el hilo de IO

class RestaurantRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val localRestaurantDataSource: LocalRestaurantDataSource, // Inyectar el DataSource local
    private val connectivityMonitor: ConnectivityMonitor // Inyectar el monitor de conectividad
) : RestaurantRepository {

    // Un CoroutineScope para lanzar tareas de sincronización en segundo plano
    // Asegura que las operaciones del repositorio se hagan en Dispatchers.IO
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        // Observar la conectividad para intentar sincronizar cuando haya red
        repositoryScope.launch {
            connectivityMonitor.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable) {
                    Log.d("RestaurantRepositoryImpl", "Network available. Attempting to sync pending restaurant updates.")
                    syncPendingRestaurantUpdates()
                }
            }
        }
    }

    override suspend fun getRestaurants(): List<RestaurantDTO> = withContext(Dispatchers.IO) {
        // Aquí podrías añadir lógica de caché si la necesitas para getRestaurants
        // Por ahora, solo llama a la API
        return@withContext apiService.getRestaurants()
    }

    override suspend fun getRestaurant(id: String): RestaurantDTO = withContext(Dispatchers.IO) {
        // Aquí podrías añadir lógica de caché si la necesitas para getRestaurant
        // Por ahora, solo llama a la API
        return@withContext apiService.getRestaurant(id)
    }

    override suspend fun updateRestaurant(restaurantId: String, updateRestaurantDTO: UpdateRestaurantDTO): Boolean = withContext(Dispatchers.IO) {
        // Intentar enviar al servidor primero
        if (connectivityMonitor.isNetworkAvailable.first()) {
            try {
                val response = apiService.updateRestaurant(restaurantId, updateRestaurantDTO)
                if (response.isSuccessful) {
                    Log.d("RestaurantRepositoryImpl", "Restaurant $restaurantId updated successfully on server.")
                    return@withContext true
                } else {
                    Log.e("RestaurantRepositoryImpl", "Failed to update restaurant $restaurantId on server: ${response.code()} - ${response.errorBody()?.string()}. Saving locally.")
                    // Si falla el servidor, guardar localmente
                    localRestaurantDataSource.savePendingUpdate(restaurantId, updateRestaurantDTO)
                    return@withContext false // Retorna false porque la actualización no fue confirmada por el servidor
                }
            } catch (e: Exception) {
                Log.e("RestaurantRepositoryImpl", "Exception updating restaurant $restaurantId on server: ${e.message}. Saving locally.", e)
                // Si hay una excepción (ej. sin red), guardar localmente
                localRestaurantDataSource.savePendingUpdate(restaurantId, updateRestaurantDTO)
                return@withContext false // Retorna false porque la actualización no fue confirmada por el servidor
            }
        } else {
            // Si no hay red, guardar directamente localmente
            Log.d("RestaurantRepositoryImpl", "No network. Saving restaurant $restaurantId update locally.")
            localRestaurantDataSource.savePendingUpdate(restaurantId, updateRestaurantDTO)
            return@withContext false // Retorna false porque la actualización no fue confirmada por el servidor
        }
    }

    override suspend fun searchRestaurants(query: String): List<RestaurantDTO> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = apiService.searchRestaurants(query)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e("RestaurantRepositoryImpl", "Error HTTP in searchRestaurants: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepositoryImpl", "Exception in searchRestaurants: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun updateRestaurantComments(restaurantId: String, commentsIds: List<String>): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val updateDto = UpdateRestaurantCommentsDTO(commentsIds)
            val response = apiService.updateRestaurantComments(restaurantId, updateDto)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("RestaurantRepositoryImpl", "Error updating restaurant comments: ${e.message}", e)
            false
        }
    }

    // Función para sincronizar las actualizaciones pendientes con el servidor
    private suspend fun syncPendingRestaurantUpdates() = withContext(Dispatchers.IO) {
        localRestaurantDataSource.getAllPendingUpdates().first().forEach { pendingUpdateRealmModel ->
            try {
                val updateDTO = localRestaurantDataSource.deserializeUpdatePayload(pendingUpdateRealmModel.updatePayloadJson)
                val response = apiService.updateRestaurant(pendingUpdateRealmModel.restaurantId, updateDTO)
                if (response.isSuccessful) {
                    Log.d("RestaurantRepositoryImpl", "Synced pending update for restaurant ${pendingUpdateRealmModel.restaurantId}. Deleting local record.")
                    localRestaurantDataSource.deletePendingUpdate(pendingUpdateRealmModel._id.toHexString())
                } else {
                    Log.e("RestaurantRepositoryImpl", "Failed to sync pending update for restaurant ${pendingUpdateRealmModel.restaurantId}: ${response.code()} - ${response.errorBody()?.string()}")
                    // Podrías añadir lógica de reintento o manejo de errores aquí
                }
            } catch (e: Exception) {
                Log.e("RestaurantRepositoryImpl", "Exception syncing pending update for restaurant ${pendingUpdateRealmModel.restaurantId}: ${e.message}", e)
                // Podrías añadir lógica de reintento o manejo de errores aquí
            }
        }
    }
}