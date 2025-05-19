package com.example.campusbites.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.restaurantDetailDataStore: DataStore<Preferences> by preferencesDataStore(name = "restaurant_detail_prefs")

@Singleton
class RestaurantPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.restaurantDetailDataStore

    private object PreferencesKeys {
        val LAST_SELECTED_TAB_INDEX = intPreferencesKey("last_selected_tab_index")
    }

    val lastSelectedTabIndexFlow: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("RestaurantPrefsRepo", "Error reading tab preferences.", exception)
                emit(emptyPreferences())
            } else {
                Log.e("RestaurantPrefsRepo", "Non-IO error reading tab preferences.", exception)
                emit(emptyPreferences()) // O re-throw si es crítico
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SELECTED_TAB_INDEX] ?: 0 // Default to tab 0
        }

    suspend fun saveLastSelectedTabIndex(index: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_SELECTED_TAB_INDEX] = index
                Log.d("RestaurantPrefsRepo", "Saved last selected tab index: $index")
            }
        } catch (e: Exception) {
            Log.e("RestaurantPrefsRepo", "Error saving last selected tab index", e)
        }
    }
}