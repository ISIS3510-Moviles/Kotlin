package com.example.campusbites.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.campusbites.data.preferences.UserSessionRepository
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.CreateUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.usecase.user.GetUserByEmailUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.io.IOException // Importar IOException para manejar errores de red

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val createUserUseCase: CreateUserUseCase,
    private val getUserByEmailUseCase: GetUserByEmailUseCase,
    private val userSessionRepository: UserSessionRepository,
    private val updateUserUseCase: UpdateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val connectivityManager: ConnectivityManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserDomain?>(null)
    override val currentUser: StateFlow<UserDomain?> = _currentUser.asStateFlow()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val isOnline = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                trySend(true)
            }

            override fun onLost(network: android.net.Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        trySend(isConnected)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }.distinctUntilChanged()


    init {
        Log.d("AuthRepository_Instance", "AuthRepositoryImpl Singleton instance created: ${this.hashCode()}")
        repositoryScope.launch {
            try {
                val initialUser = userSessionRepository.userSessionFlow.firstOrNull()
                if (initialUser != null && _currentUser.value == null) {
                    Log.d("AuthRepository", "Restoring session from DataStore for user: ${initialUser.id} with role: ${initialUser.role}")
                    _currentUser.value = initialUser
                    // **MODIFICACIÓN 1: Intentar sincronizar al inicio si hay conexión**
                    // Esto cubre el caso de que la app se cerró mientras estaba offline con un cambio pendiente.
                    if (isOnline.firstOrNull() == true) {
                        Log.d("AuthRepository", "Network is online at startup. Attempting to sync restored user.")
                        syncLocalUserWithRemote()
                    } else {
                        Log.d("AuthRepository", "Network is offline at startup. User will sync when online.")
                    }
                } else if (initialUser == null) {
                    Log.d("AuthRepository", "No session found in DataStore.")
                } else {
                    Log.d("AuthRepository", "Session already loaded or being loaded.")
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error restoring session from DataStore: ${e.message}", e)
            }
        }

        repositoryScope.launch {
            isOnline
                .filter { it } // Solo cuando la red está online
                .collect {
                    Log.d("AuthRepository", "Network is back online. Attempting to sync local user data.")
                    syncLocalUserWithRemote()
                }
        }
    }

    override suspend fun performCheckOrCreateUser(
        userId: String,
        userName: String,
        userEmail: String
    ): UserDomain = withContext(Dispatchers.IO) {

        try {
            Log.d("AuthRepository", "🔎 Buscando usuario con email: $userEmail")

            val existingUser = try {
                getUserByEmailUseCase(userEmail)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error searching for user by email: ${e.message}", e)
                null
            }
            Log.d("AuthRepository", "🔍 Usuario encontrado? ${existingUser != null}")

            val userToSet: UserDomain = if (existingUser != null) {
                try {
                    // **MODIFICACIÓN 2: Asegurarse de obtener el UserDomain completo del servidor**
                    // Si el usuario existe, lo obtenemos por ID para tener la versión más reciente del servidor.
                    // Esto es importante si el rol pudo haber sido cambiado por otro medio en el servidor.
                    getUserByIdUseCase(existingUser.id)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error getting user by ID after finding by email: ${e.message}", e)
                    throw e
                }
            } else {
                UserDomain(
                    id = userId,
                    name = userName,
                    email = userEmail,
                    phone = "",
                    role = "user", // Rol por defecto para nuevos usuarios
                    isPremium = false,
                    badgesIds = emptyList(),
                    schedulesIds = emptyList(),
                    reservationsDomain = emptyList(),
                    institution = null,
                    dietaryPreferencesTagIds = emptyList(),
                    commentsIds = emptyList(),
                    visitsIds = emptyList(),
                    suscribedRestaurantIds = emptyList(),
                    publishedAlertsIds = emptyList(),
                    savedProducts = emptyList(),
                    vendorRestaurantId = null
                ).also { newUser ->
                    Log.d("AuthRepository", "🚀 Creando usuario nuevo…")
                    try {
                        createUserUseCase(newUser)
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Error creating new user: ${e.message}", e)
                        throw e
                    }
                }
            }

            Log.d("AuthRepository", "✅ Usuario listo: ${userToSet.id}. Guardando sesión.")
            _currentUser.value = userToSet
            try {
                userSessionRepository.saveUserSession(userToSet)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error saving user session: ${e.message}", e)
            }

            userToSet
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error en performCheckOrCreateUser: ${e.message}", e)
            throw e
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            Log.d("AuthRepository", "🗑️ Clearing user state in Repository and DataStore.")
            _currentUser.value = null
            try {
                userSessionRepository.clearUserSession()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error clearing user session: ${e.message}", e)
            }
        }
    }

    override fun updateCurrentUser(updatedUser: UserDomain?) {
        Log.d("AuthRepository", "🔄 Updating currentUser in repository with: ${updatedUser?.id}, new role: ${updatedUser?.role}")
        _currentUser.value = updatedUser?.copy() // Asegura que se actualice el Flow para la UI

        if (updatedUser != null) {
            repositoryScope.launch {
                try {
                    // 1. Guardar el usuario actualizado en DataStore (persistencia offline)
                    userSessionRepository.saveUserSession(updatedUser)
                    Log.d("AuthRepository", "🔄 User session updated in DataStore for ID: ${updatedUser.id}")

                    // 2. Intentar sincronizar con el servidor inmediatamente
                    // Si hay conexión, se enviará. Si no, syncLocalUserWithRemote lo hará después.
                    if (isOnline.firstOrNull() == true) { // Verifica si hay conexión antes de intentar
                        Log.d("AuthRepository", "🚀 Attempting to update user on server...")
                        try {
                            updateUserUseCase(updatedUser.id, updatedUser)
                            Log.d("AuthRepository", "✅ User successfully updated on server.")
                        } catch (e: IOException) { // Captura IOException para errores de red
                            Log.e("AuthRepository", "❌ Failed to update user on server (network error): ${e.message}", e)
                            // No re-lanzamos, ya que syncLocalUserWithRemote lo reintentará
                        } catch (e: Exception) { // Captura otras excepciones (ej. errores de servidor)
                            Log.e("AuthRepository", "❌ Failed to update user on server (other error): ${e.message}", e)
                            // Aquí podrías decidir si quieres reintentar o notificar al usuario
                            // Por ahora, el syncLocalUserWithRemote solo reintenta por red.
                        }
                    } else {
                        Log.d("AuthRepository", "🌐 Offline. User update will sync when network is available.")
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error saving updated user session to DataStore: ${e.message}", e)
                }
            }
        } else {
            repositoryScope.launch {
                try {
                    userSessionRepository.clearUserSession()
                    Log.d("AuthRepository", "🔄 User is null, clearing session in DataStore.")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error clearing user session when user is null: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun getCurrentUser(): UserDomain? {
        return withContext(Dispatchers.IO) {
            _currentUser.value
        }
    }

    private suspend fun syncLocalUserWithRemote() {
        val localUser = _currentUser.value
        if (localUser != null) {
            Log.d("AuthRepository", "Attempting to sync local user ${localUser.id} with remote. Current role: ${localUser.role}")
            try {
                // Aquí es donde se reintenta el envío al servidor
                updateUserUseCase(localUser.id, localUser)
                Log.d("AuthRepository", "Local user ${localUser.id} successfully synced with remote.")
            } catch (e: IOException) { // Captura IOException para errores de red
                Log.e("AuthRepository", "Failed to sync local user ${localUser.id} with remote (network error): ${e.message}", e)
                // No re-lanzamos, ya que el `isOnline` Flow se encargará de reintentar cuando la red vuelva.
            } catch (e: Exception) { // Captura otras excepciones (ej. errores de servidor)
                Log.e("AuthRepository", "Failed to sync local user ${localUser.id} with remote (other error): ${e.message}", e)
                // Aquí podrías decidir si quieres notificar al usuario o tener una lógica de reintento más sofisticada
                // para errores que no sean de red. Por ahora, solo logueamos.
            }
        } else {
            Log.d("AuthRepository", "No local user to sync.")
        }
    }
}