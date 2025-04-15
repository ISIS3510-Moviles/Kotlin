package com.example.campusbites.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.campusbites.data.preferences.homeDataStore
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

@Singleton
class HomeDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val dataStore = context.homeDataStore

    private object PreferencesKeys {
        val NEARBY_RESTAURANTS_JSON = stringPreferencesKey("nearby_restaurants_json")
        val RECOMMENDED_RESTAURANTS_JSON = stringPreferencesKey("recommended_restaurants_json")
        val ALL_PRODUCTS_JSON = stringPreferencesKey("all_products_json")
        val ALL_INGREDIENTS_JSON = stringPreferencesKey("all_ingredients_json")
    }

    private inline fun <reified T> decodeListSafely(jsonString: String?): List<T> {
        return if (jsonString != null) {
            try {
                val listSerializer = ListSerializer(serializer<T>())
                json.decodeFromString(listSerializer, jsonString)
            } catch (e: Exception) {
                Log.e("HomeDataRepo", "Error decoding list JSON for ${T::class.simpleName}", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val nearbyRestaurantsFlow: Flow<List<RestaurantDomain>> = dataStore.data
        .catch { exception ->
            Log.e("HomeDataRepo", "Error reading nearby restaurants preferences.", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            decodeListSafely<RestaurantDomain>(preferences[PreferencesKeys.NEARBY_RESTAURANTS_JSON])
        }

    val recommendedRestaurantsFlow: Flow<List<RestaurantDomain>> = dataStore.data
        .catch { exception ->
            Log.e("HomeDataRepo", "Error reading recommended restaurants preferences.", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            decodeListSafely<RestaurantDomain>(preferences[PreferencesKeys.RECOMMENDED_RESTAURANTS_JSON])
        }

    val allProductsFlow: Flow<List<ProductDomain>> = dataStore.data
        .catch { exception ->
            Log.e("HomeDataRepo", "Error reading all products preferences.", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            decodeListSafely<ProductDomain>(preferences[PreferencesKeys.ALL_PRODUCTS_JSON])
        }

    val allIngredientsFlow: Flow<List<IngredientDomain>> = dataStore.data
        .catch { exception ->
            Log.e("HomeDataRepo", "Error reading all ingredients preferences.", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            decodeListSafely<IngredientDomain>(preferences[PreferencesKeys.ALL_INGREDIENTS_JSON])
        }


    suspend fun saveNearbyRestaurants(restaurants: List<RestaurantDomain>) {
        try {
            val jsonString = json.encodeToString(ListSerializer(RestaurantDomain.serializer()), restaurants)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.NEARBY_RESTAURANTS_JSON] = jsonString
                Log.d("HomeDataRepo", "Saved ${restaurants.size} nearby restaurants to preferences.")
            }
        } catch (e: Exception) {
            Log.e("HomeDataRepo", "Error encoding nearby restaurants JSON", e)
        }
    }

    suspend fun saveRecommendedRestaurants(restaurants: List<RestaurantDomain>) {
        try {
            val jsonString = json.encodeToString(ListSerializer(RestaurantDomain.serializer()), restaurants)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.RECOMMENDED_RESTAURANTS_JSON] = jsonString
                Log.d("HomeDataRepo", "Saved ${restaurants.size} recommended restaurants to preferences.")
            }
        } catch (e: Exception) {
            Log.e("HomeDataRepo", "Error encoding recommended restaurants JSON", e)
        }
    }

    suspend fun saveAllProducts(products: List<ProductDomain>) {
        try {
            val jsonString = json.encodeToString(ListSerializer(ProductDomain.serializer()), products)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALL_PRODUCTS_JSON] = jsonString
                Log.d("HomeDataRepo", "Saved ${products.size} products to preferences.")
            }
        } catch (e: Exception) {
            Log.e("HomeDataRepo", "Error encoding products JSON", e)
        }
    }

    suspend fun saveAllIngredients(ingredients: List<IngredientDomain>) {
        try {
            val jsonString = json.encodeToString(ListSerializer(IngredientDomain.serializer()), ingredients)
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ALL_INGREDIENTS_JSON] = jsonString
                Log.d("HomeDataRepo", "Saved ${ingredients.size} ingredients to preferences.")
            }
        } catch (e: Exception) {
            Log.e("HomeDataRepo", "Error encoding ingredients JSON", e)
        }
    }

    suspend fun clearHomeData() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.NEARBY_RESTAURANTS_JSON)
            preferences.remove(PreferencesKeys.RECOMMENDED_RESTAURANTS_JSON)
            preferences.remove(PreferencesKeys.ALL_PRODUCTS_JSON)
            preferences.remove(PreferencesKeys.ALL_INGREDIENTS_JSON)
            Log.d("HomeDataRepo", "Cleared home data preferences.")
        }
    }
}