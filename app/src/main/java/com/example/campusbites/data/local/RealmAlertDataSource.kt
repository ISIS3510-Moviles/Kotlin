// package com.example.campusbites.data.local (o donde corresponda)
package com.example.campusbites.data.local

import com.example.campusbites.data.local.realm.RealmConfig
import com.example.campusbites.data.local.realm.model.AlertRealmModel
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import io.realm.kotlin.UpdatePolicy
import io.realm.kotlin.ext.query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

interface LocalAlertDataSource {
    fun getAlertsFlow(): Flow<List<AlertDomain>>
    suspend fun saveAlerts(alerts: List<AlertDomain>)
    suspend fun deleteAllAlerts()
    suspend fun updateAlert(alert: AlertDomain)
}

@Singleton
class RealmAlertDataSource @Inject constructor(
    private val realmConfig: RealmConfig
) : LocalAlertDataSource {

    private fun mapRealmToDomain(realmModel: AlertRealmModel): AlertDomain {
        // Para publisher y restaurant, creamos instancias simplificadas
        // ya que no almacenamos todos sus detalles en AlertRealmModel.
        // Si se necesita el objeto completo, se debería buscar por ID.
        val publisher = UserDomain(
            id = realmModel.publisherId,
            name = realmModel.publisherName,
            // Rellenar otros campos con valores por defecto o vacíos si es necesario
            // para la visualización básica desde la caché.
            phone = "", email = "", role = "", isPremium = false, badgesIds = emptyList(),
            schedulesIds = emptyList(), reservationsDomain = emptyList(), institution = null,
            dietaryPreferencesTagIds = emptyList(), commentsIds = emptyList(), visitsIds = emptyList(),
            suscribedRestaurantIds = emptyList(), publishedAlertsIds = emptyList(), savedProducts = emptyList()
        )

        val restaurant = RestaurantDomain(
            id = realmModel.restaurantId,
            name = realmModel.restaurantName,
            // Rellenar otros campos con valores por defecto o vacíos
            description = "", latitude = 0.0, longitude = 0.0, routeIndications = "",
            openingTime = "", closingTime = "", opensHolidays = false, opensWeekends = false,
            isActive = false, rating = 0.0, address = "", phone = "", email = "",
            overviewPhoto = "", profilePhoto = "", photos = emptyList(), foodTags = emptyList(),
            dietaryTags = emptyList(), alertsIds = emptyList(), reservationsIds = emptyList(),
            suscribersIds = emptyList(), visitsIds = emptyList(), commentsIds = emptyList(),
            productsIds = emptyList()
        )

        return AlertDomain(
            id = realmModel.id,
            datetime = Instant.parse(realmModel.datetime).atZone(ZoneOffset.UTC).toLocalDateTime(),
            icon = realmModel.icon,
            message = realmModel.message,
            votes = realmModel.votes,
            publisher = publisher,
            restaurantDomain = restaurant
        )
    }

    private fun mapDomainToRealm(domainModel: AlertDomain): AlertRealmModel {
        return AlertRealmModel(
            id = domainModel.id,
            datetime = domainModel.datetime.toInstant(ZoneOffset.UTC).toString(),
            icon = domainModel.icon,
            message = domainModel.message,
            votes = domainModel.votes,
            publisherId = domainModel.publisher.id,
            publisherName = domainModel.publisher.name, // Guardar nombre para fácil acceso
            restaurantId = domainModel.restaurantDomain.id,
            restaurantName = domainModel.restaurantDomain.name // Guardar nombre
        )
    }

    override fun getAlertsFlow(): Flow<List<AlertDomain>> {
        return realmConfig.realm.query<AlertRealmModel>().find().asFlow()
            .map { resultsChange ->
                resultsChange.list.map { mapRealmToDomain(it) }
            }
    }

    override suspend fun saveAlerts(alerts: List<AlertDomain>) {
        val realm = realmConfig.realm
        realm.write {
            alerts.forEach { domainAlert ->
                val realmAlert = mapDomainToRealm(domainAlert)
                copyToRealm(realmAlert, UpdatePolicy.ALL)
            }
        }
    }

    override suspend fun updateAlert(alert: AlertDomain) {
        val realm = realmConfig.realm
        realm.write {
            val realmAlert = mapDomainToRealm(alert)
            copyToRealm(realmAlert, UpdatePolicy.ALL)
        }
    }

    override suspend fun deleteAllAlerts() {
        val realm = realmConfig.realm
        realm.write {
            val allAlerts = query<AlertRealmModel>().find()
            delete(allAlerts)
        }
    }
}