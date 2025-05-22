package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.AlertDTO
import com.example.campusbites.data.dto.CreateAlertDTO
import com.example.campusbites.data.dto.RestaurantDTO
import com.example.campusbites.data.dto.UpdateAlertDTO
import com.example.campusbites.data.dto.UserDTO
import com.example.campusbites.data.local.LocalAlertDataSource
import com.example.campusbites.data.mapper.AlertMapper
import com.example.campusbites.data.mapper.RestaurantMapper
import com.example.campusbites.data.mapper.UserMapper
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AlertRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class AlertRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val localAlertDataSource: LocalAlertDataSource,
    private val userMapper: UserMapper,
    private val restaurantMapper: RestaurantMapper,
    private val alertMapper: AlertMapper
    // getReservationByIdUseCase y getInstitutionByIdUseCase se usan dentro de UserMapper
) : AlertRepository {

    // Función auxiliar para obtener UserDTOs en "batch" (simulado con llamadas concurrentes)
    private suspend fun fetchUserDTOsConcurrently(userIds: List<String>): Map<String, UserDTO?> {
        return coroutineScope {
            userIds.map { id ->
                async {
                    try {
                        Log.d("AlertRepoImpl", "Fetching UserDTO for id: $id")
                        apiService.getUserById(id)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Failed to fetch UserDTO for id: $id. Error: ${e.message}", e)
                        null // Devolver null si falla para este ID específico
                    }
                }
            }.awaitAll().mapIndexedNotNull { index, userDto ->
                userIds[index] to userDto // Crea pares (ID, UserDTO?)
            }.toMap()
        }
    }

    // Función auxiliar para obtener RestaurantDTOs en "batch" (simulado con llamadas concurrentes)
    private suspend fun fetchRestaurantDTOsConcurrently(restaurantIds: List<String>): Map<String, RestaurantDTO?> {
        return coroutineScope {
            restaurantIds.map { id ->
                async {
                    try {
                        Log.d("AlertRepoImpl", "Fetching RestaurantDTO for id: $id")
                        if (id.isNotBlank()) apiService.getRestaurant(id) else null
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Failed to fetch RestaurantDTO for id: $id. Error: ${e.message}", e)
                        null
                    }
                }
            }.awaitAll().mapIndexedNotNull { index, restaurantDto ->
                restaurantIds[index] to restaurantDto
            }.toMap()
        }
    }


    override suspend fun fetchAndSaveRemoteAlerts(): List<AlertDomain> {
        return try {
            val alertDtos: List<AlertDTO> = apiService.getAlerts()
            Log.d("AlertRepoImpl", "Fetched ${alertDtos.size} alert DTOs from API.")
            if (alertDtos.isEmpty()) return emptyList()

            val uniquePublisherIds = alertDtos.map { it.publisherId }.distinct().filter { it.isNotBlank() }
            val uniqueRestaurantIds = alertDtos.map { it.restaurantId }.distinct().filter { it.isNotBlank() }

            Log.d("AlertRepoImpl", "Unique publisher IDs: $uniquePublisherIds")
            Log.d("AlertRepoImpl", "Unique restaurant IDs: $uniqueRestaurantIds")

            // Obtener todos los UserDTOs y RestaurantDTOs necesarios
            val userDtosMap = if (uniquePublisherIds.isNotEmpty()) fetchUserDTOsConcurrently(uniquePublisherIds) else emptyMap()
            val restaurantDtosMap = if (uniqueRestaurantIds.isNotEmpty()) fetchRestaurantDTOsConcurrently(uniqueRestaurantIds) else emptyMap()

            Log.d("AlertRepoImpl", "Fetched ${userDtosMap.size} user DTOs and ${restaurantDtosMap.size} restaurant DTOs.")

            // Mapear DTOs a Domain Models (User y Restaurant)
            // Esta parte todavía puede tener N+1 internos si los mappers hacen llamadas para dependencias (instituciones, reservas, tags)
            val usersDomainMap = userDtosMap.mapValuesNotNull { entry ->
                entry.value?.let { userDto ->
                    try {
                        userMapper.mapDtoToDomain(userDto)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Error mapping UserDTO ${entry.key} to UserDomain: ${e.message}", e)
                        userMapper.createFallbackUser(entry.key) // Usar fallback
                    }
                }
            }
            val restaurantsDomainMap = restaurantDtosMap.mapValuesNotNull { entry ->
                entry.value?.let { restaurantDto ->
                    try {
                        restaurantMapper.mapDtoToDomain(restaurantDto)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Error mapping RestaurantDTO ${entry.key} to RestaurantDomain: ${e.message}", e)
                        restaurantMapper.createFallbackRestaurant(entry.key) // Usar fallback
                    }
                }
            }

            Log.d("AlertRepoImpl", "Mapped ${usersDomainMap.size} UserDomains and ${restaurantsDomainMap.size} RestaurantDomains.")

            val domainAlerts = alertDtos.mapNotNull { alertDto ->
                try {
                    val publisher = usersDomainMap[alertDto.publisherId]
                        ?: userMapper.createFallbackUser(alertDto.publisherId)
                    val restaurant = restaurantsDomainMap[alertDto.restaurantId]
                        ?: restaurantMapper.createFallbackRestaurant(alertDto.restaurantId, alertDto.id)

                    alertMapper.mapDtoToDomain(alertDto, publisher, restaurant)
                } catch (e: Exception) {
                    Log.e("AlertRepoImpl", "Failed to map AlertDTO ${alertDto.id} to AlertDomain. Error: ${e.message}", e)
                    null
                }
            }

            if (alertDtos.isNotEmpty() && domainAlerts.size < alertDtos.size) {
                Log.w("AlertRepoImpl", "${alertDtos.size - domainAlerts.size} alert DTOs failed to map to Domain objects and were skipped.")
            }

            localAlertDataSource.deleteAllAlerts()
            localAlertDataSource.saveAlerts(domainAlerts)
            Log.d("AlertRepositoryImpl", "Fetched and saved ${domainAlerts.size} alerts to Realm.")
            domainAlerts
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error fetching remote alerts: ${e.message}", e)
            throw e // Relanzar para que el ViewModel lo maneje
        }
    }


    override fun getLocalAlertsFlow(): Flow<List<AlertDomain>> {
        return localAlertDataSource.getAlertsFlow()
    }

    override suspend fun getAlertById(id: String): AlertDomain? {
        // Podríamos optimizar esto para buscar primero en local y luego en remoto si no se encuentra,
        // pero la interfaz actual no lo sugiere. Mantenemos la lógica de buscar en el flujo local.
        return localAlertDataSource.getAlertsFlow().firstOrNull()?.find { it.id == id }
    }

    override suspend fun updateAlertVotes(id: String, votes: Int): Boolean {
        val updateDto = UpdateAlertDTO(votes = votes)
        return try {
            val success = apiService.updateAlertVotes(id, updateDto)
            if (success) {
                val alertToUpdate = getAlertById(id) // Obtiene de la caché local/Realm
                alertToUpdate?.let {
                    val updatedAlert = it.copy(votes = votes)
                    localAlertDataSource.updateAlert(updatedAlert) // Actualiza en Realm
                } ?: Log.w("AlertRepositoryImpl", "Alert $id not found locally to update votes after API success.")
            }
            success
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error updating alert votes for $id: ${e.message}", e)
            false
        }
    }

    override suspend fun createAlert(
        datetime: String,
        icon: String,
        message: String,
        publisherId: String,
        restaurantId: String
    ): AlertDomain {
        val createAlertDTO = CreateAlertDTO(
            datetime = datetime,
            icon = icon,
            message = message,
            publisherId = publisherId,
            restaurantId = restaurantId,
            votes = 0
        )
        try {
            Log.d("AlertRepoImpl", "Creating alert with restaurantId: $restaurantId, publisherId: $publisherId")
            val createdAlertDTO = apiService.createAlert(createAlertDTO)
            Log.d("AlertRepoImpl", "Alert DTO created successfully, ID: ${createdAlertDTO.id}. Now mapping to domain.")

            // Obtener UserDTO y RestaurantDTO para el mapeo
            val userDto = try { apiService.getUserById(createdAlertDTO.publisherId) } catch (e: Exception) {
                Log.e("AlertRepoImpl", "Failed to fetch UserDTO for created alert ${createdAlertDTO.id}", e); null
            }
            val restaurantDto = if (createdAlertDTO.restaurantId.isNotBlank()) {
                try { apiService.getRestaurant(createdAlertDTO.restaurantId) } catch (e: Exception) {
                    Log.e("AlertRepoImpl", "Failed to fetch RestaurantDTO for created alert ${createdAlertDTO.id}", e); null
                }
            } else null

            val publisherDomain = userDto?.let { userMapper.mapDtoToDomain(it) }
                ?: userMapper.createFallbackUser(createdAlertDTO.publisherId)
            val restaurantDomain = restaurantDto?.let { restaurantMapper.mapDtoToDomain(it) }
                ?: restaurantMapper.createFallbackRestaurant(createdAlertDTO.restaurantId, createdAlertDTO.id)

            val domainAlert = alertMapper.mapDtoToDomain(createdAlertDTO, publisherDomain, restaurantDomain)

            Log.d("AlertRepoImpl", "Alert mapped to domain. Saving to local data source.")
            localAlertDataSource.updateAlert(domainAlert) // Guardar/Actualizar en Realm
            Log.d("AlertRepoImpl", "Alert saved locally. ID: ${domainAlert.id}")
            return domainAlert
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error creating alert: ${e.message}", e)
            throw e
        }
    }

    // Helper para mapValues que no incluye entradas donde el valor es null
    private inline fun <K, V, R> Map<K, V>.mapValuesNotNull(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
        val result = LinkedHashMap<K, R>()
        for (entry in this) {
            transform(entry)?.let {
                result[entry.key] = it
            }
        }
        return result
    }
}