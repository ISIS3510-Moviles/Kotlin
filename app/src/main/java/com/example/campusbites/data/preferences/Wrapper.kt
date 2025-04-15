package com.example.campusbites.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.campusbites.domain.model.UserDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Singleton
class UserSessionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val USER_DATA_JSON = stringPreferencesKey("user_data_json")
    }

    val userSessionFlow: Flow<UserDomain?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("UserSessionRepo", "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                Log.e("UserSessionRepo", "Non-IO error reading preferences.", exception)
                emit(emptyPreferences())
            }
        }
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.USER_DATA_JSON]
            if (jsonString != null) {
                try {
                    json.decodeFromString<UserDomain>(jsonString)
                } catch (e: Exception) {
                    Log.e("UserSessionRepo", "Error decoding user JSON", e)
                    null
                }
            } else {
                null
            }
        }

    suspend fun saveUserSession(user: UserDomain) {
        try {
            val jsonString = json.encodeToString(user)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USER_DATA_JSON] = jsonString
                Log.d("UserSessionRepo", "User session saved for ID: ${user.id}")
            }
        } catch (e: Exception) {
            Log.e("UserSessionRepo", "Error encoding user JSON", e)
        }
    }

    suspend fun clearUserSession() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_DATA_JSON)
            Log.d("UserSessionRepo", "User session cleared.")
        }
    }
}