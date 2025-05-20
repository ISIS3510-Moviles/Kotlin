package com.example.campusbites.data.cache

import android.util.ArrayMap
import android.util.Log
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResults(
    val products: List<ProductDomain>,
    val restaurants: List<RestaurantDomain>
)

@Singleton
class SearchCache @Inject constructor() {
    private val cache: ArrayMap<String, SearchResults> = ArrayMap()
    private val maxSize = 20 // Limitar el número de consultas cacheadas

    init {
        Log.d("SearchCache", "SearchCache initialized with maxSize: $maxSize")
    }

    fun get(query: String): SearchResults? {
        val key = query.lowercase().trim()
        val results = cache[key]
        if (results != null) {
            Log.d("SearchCache", "Cache HIT for query: '$key'")
        } else {
            Log.d("SearchCache", "Cache MISS for query: '$key'")
        }
        return results
    }

    fun put(query: String, results: SearchResults) {
        val key = query.lowercase().trim()
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            // Evicción simple: eliminar el más antiguo si se alcanza maxSize y es una nueva consulta
            val oldestKey = cache.keyAt(0)
            cache.removeAt(0)
            Log.d("SearchCache", "Cache full. Evicted: '$oldestKey'. Caching new query: '$key'")
        } else if (cache.containsKey(key)) {
            Log.d("SearchCache", "Updating cache for query: '$key'")
        } else {
            Log.d("SearchCache", "Caching new query: '$key'")
        }
        cache[key] = results
    }

    fun clear() {
        Log.d("SearchCache", "Clearing SearchCache")
        cache.clear()
    }
}