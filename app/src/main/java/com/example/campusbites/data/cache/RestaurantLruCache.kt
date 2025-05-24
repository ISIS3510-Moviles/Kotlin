package com.example.campusbites.data.cache

import android.util.Log
import android.util.LruCache
import com.example.campusbites.domain.model.RestaurantDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantLruCache @Inject constructor() {
    // Obtener la memoria máxima que la JVM puede usar en kilobytes
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    // Usar 1/8 de la memoria disponible para este caché
    private val cacheSize = maxMemory / 8

    private val lru: LruCache<String, RestaurantDomain> = LruCache(cacheSize)

    init {
        Log.d("RestaurantLruCache", "RestaurantLruCache initialized with size: $cacheSize KB")
    }

    fun get(id: String): RestaurantDomain? {
        val restaurant = lru.get(id)
        if (restaurant != null) {
            Log.d("RestaurantLruCache", "Cache HIT for restaurant ID: $id")
        } else {
            Log.d("RestaurantLruCache", "Cache MISS for restaurant ID: $id")
        }
        return restaurant
    }

    fun put(id: String, restaurant: RestaurantDomain) {
        Log.d("RestaurantLruCache", "Caching restaurant ID: $id")
        lru.put(id, restaurant)
    }

    fun clear() {
        Log.d("RestaurantLruCache", "Clearing RestaurantLruCache")
        lru.evictAll()
    }
}