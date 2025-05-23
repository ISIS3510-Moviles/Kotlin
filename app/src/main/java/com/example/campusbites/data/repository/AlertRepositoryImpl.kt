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
) : AlertRepository {

    private suspend fun fetchUserDTOsConcurrently(userIds: List<String>): Map<String, UserDTO?> {
        return coroutineScope {
            userIds.map { id ->
                async {
                    try {
                        Log.d("AlertRepoImpl", "Fetching UserDTO for id: $id")
                        apiService.getUserById(id)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Failed to fetch UserDTO for id: $id. Error: ${e.message}", e)
                        null
                    }
                }
            }.awaitAll().mapIndexedNotNull { index, userDto ->
                userIds[index] to userDto
            }.toMap()
        }
    }

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

            val validAlertDtos = alertDtos.filter { dto ->
                if (dto.id == null) {
                    Log.w("AlertRepoImpl", "AlertDTO received from API with null ID. Publisher: ${dto.publisherId}, Restaurant: ${dto.restaurantId}, Message: ${dto.message.take(50)}. Skipping this alert.")
                    false
                } else {
                    true
                }
            }

            if (validAlertDtos.isEmpty() && alertDtos.isNotEmpty()) {
                Log.w("AlertRepoImpl", "All ${alertDtos.size} fetched AlertDTOs had null IDs or other critical missing fields. No alerts will be processed.")
                localAlertDataSource.deleteAllAlerts()
                return emptyList()
            }
            if (validAlertDtos.size < alertDtos.size) {
                Log.w("AlertRepoImpl", "${alertDtos.size - validAlertDtos.size} AlertDTOs were filtered out due to null ID or other critical missing fields.")
            }

            if (validAlertDtos.isEmpty()) return emptyList()


            val uniquePublisherIds = validAlertDtos.map { it.publisherId }.distinct().filter { it.isNotBlank() }
            val uniqueRestaurantIds = validAlertDtos.map { it.restaurantId }.distinct().filter { it.isNotBlank() }

            Log.d("AlertRepoImpl", "Unique publisher IDs for valid alerts: $uniquePublisherIds")
            Log.d("AlertRepoImpl", "Unique restaurant IDs for valid alerts: $uniqueRestaurantIds")

            val userDtosMap = if (uniquePublisherIds.isNotEmpty()) fetchUserDTOsConcurrently(uniquePublisherIds) else emptyMap()
            val restaurantDtosMap = if (uniqueRestaurantIds.isNotEmpty()) fetchRestaurantDTOsConcurrently(uniqueRestaurantIds) else emptyMap()

            Log.d("AlertRepoImpl", "Fetched ${userDtosMap.size} user DTOs and ${restaurantDtosMap.size} restaurant DTOs for valid alerts.")

            val usersDomainMap = userDtosMap.mapValuesNotNull { entry ->
                entry.value?.let { userDto ->
                    try {
                        userMapper.mapDtoToDomain(userDto)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Error mapping UserDTO ${entry.key} to UserDomain: ${e.message}", e)
                        userMapper.createFallbackUser(entry.key)
                    }
                }
            }
            val restaurantsDomainMap = restaurantDtosMap.mapValuesNotNull { entry ->
                entry.value?.let { restaurantDto ->
                    try {
                        restaurantMapper.mapDtoToDomain(restaurantDto)
                    } catch (e: Exception) {
                        Log.e("AlertRepoImpl", "Error mapping RestaurantDTO ${entry.key} to RestaurantDomain: ${e.message}", e)
                        restaurantMapper.createFallbackRestaurant(entry.key)
                    }
                }
            }

            Log.d("AlertRepoImpl", "Mapped ${usersDomainMap.size} UserDomains and ${restaurantsDomainMap.size} RestaurantDomains for valid alerts.")

            val domainAlerts = validAlertDtos.mapNotNull { alertDto ->
                try {
                    val publisher = usersDomainMap[alertDto.publisherId]
                        ?: userMapper.createFallbackUser(alertDto.publisherId)
                    val restaurant = restaurantsDomainMap[alertDto.restaurantId]
                        ?: restaurantMapper.createFallbackRestaurant(alertDto.restaurantId, alertDto.id!!) // id no será null aquí

                    alertMapper.mapDtoToDomain(alertDto, publisher, restaurant)
                } catch (e: Exception) {
                    // El mapper ahora puede lanzar IllegalStateException si alertDto.id es null, aunque no debería llegar aquí.
                    Log.e("AlertRepoImpl", "Failed to map valid AlertDTO ${alertDto.id} to AlertDomain. Error: ${e.message}", e)
                    null
                }
            }

            if (validAlertDtos.isNotEmpty() && domainAlerts.size < validAlertDtos.size) {
                Log.w("AlertRepoImpl", "${validAlertDtos.size - domainAlerts.size} valid alert DTOs failed to map to Domain objects and were skipped.")
            }

            localAlertDataSource.deleteAllAlerts()
            localAlertDataSource.saveAlerts(domainAlerts)
            Log.d("AlertRepositoryImpl", "Fetched and saved ${domainAlerts.size} alerts to Realm.")
            domainAlerts
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error fetching remote alerts: ${e.message}", e)
            throw e
        }
    }


    override fun getLocalAlertsFlow(): Flow<List<AlertDomain>> {
        return localAlertDataSource.getAlertsFlow()
    }

    override suspend fun getAlertById(id: String): AlertDomain? {
        return localAlertDataSource.getAlertsFlow().firstOrNull()?.find { it.id == id }
    }

    override suspend fun updateAlertVotes(id: String, votes: Int): Boolean {
        val updateDto = UpdateAlertDTO(votes = votes)
        return try {
            val success = apiService.updateAlertVotes(id, updateDto)
            if (success) {
                val alertToUpdate = getAlertById(id)
                alertToUpdate?.let {
                    val updatedAlert = it.copy(votes = votes)
                    localAlertDataSource.updateAlert(updatedAlert)
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

            if (createdAlertDTO.id == null) {
                Log.e("AlertRepoImpl", "API created an alert but returned it with a null ID. Cannot process: $createdAlertDTO")
                throw IllegalStateException("API returned created alert with null ID.")
            }
            Log.d("AlertRepoImpl", "Alert DTO created successfully, ID: ${createdAlertDTO.id}. Now mapping to domain.")


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
                ?: restaurantMapper.createFallbackRestaurant(createdAlertDTO.restaurantId, createdAlertDTO.id!!) // id no será null

            val domainAlert = alertMapper.mapDtoToDomain(createdAlertDTO, publisherDomain, restaurantDomain)

            Log.d("AlertRepoImpl", "Alert mapped to domain. Saving to local data source.")
            localAlertDataSource.updateAlert(domainAlert)
            Log.d("AlertRepoImpl", "Alert saved locally. ID: ${domainAlert.id}")
            return domainAlert
        } catch (e: Exception) {
            Log.e("AlertRepositoryImpl", "Error creating alert: ${e.message}", e)
            throw e
        }
    }

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