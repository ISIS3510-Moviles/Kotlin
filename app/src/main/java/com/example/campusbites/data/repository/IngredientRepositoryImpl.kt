package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.IngredientDTO
import com.example.campusbites.data.local.dao.IngredientDao
import com.example.campusbites.data.local.entity.IngredientEntity
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.repository.IngredientRepository
import javax.inject.Inject // Cambiado de jakarta.inject.Inject a javax.inject.Inject

class IngredientRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val ingredientDao: IngredientDao
) : IngredientRepository {

    override suspend fun getIngredients(): List<IngredientDomain> {
        return try {
            val dtos = apiService.getIngredients()
            val entities = dtos.map { dto ->
                IngredientEntity(dto.id, dto.name, dto.description, dto.image, dto.clicks)
            }
            ingredientDao.insertAll(entities)
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.w("IngredientRepository", "API call failed for getIngredients, fetching from local DB. Error: ${e.message}")
            ingredientDao.getAllOnce().map { it.toDomain() }
        }
    }

    override suspend fun getIngredientById(id: String): IngredientDomain? {
        return try {
            apiService.getIngredients().find { it.id == id }?.toDomain() // Asumiendo que no hay endpoint /ingredient/{id}
        } catch (e: Exception) {
            Log.w("IngredientRepository", "API call failed for getIngredientById($id), fetching from local DB. Error: ${e.message}")
            ingredientDao.getById(id)?.toDomain()
        }
    }

    override suspend fun incrementIngredientClicks(ingredientId: String) {
        // Optimistic update: update local first
        val localIngredient = ingredientDao.getById(ingredientId)
        if (localIngredient != null) {
            val updatedLocalIngredient = localIngredient.copy(clicks = localIngredient.clicks + 1)
            ingredientDao.update(updatedLocalIngredient)
        }

        try {
            apiService.incrementIngredientClicks(ingredientId)
            // Si la API tiene éxito, el caché local ya está (optimisticamente) actualizado.
            // Podríamos re-sincronizar desde la API si quisiéramos la "verdad" del servidor.
        } catch (e: Exception) {
            Log.e("IngredientRepository", "Failed to increment clicks on API for $ingredientId: ${e.message}")
            // Si falla la API, el cambio local persiste. Se podría encolar una acción pendiente si fuera crítico.
        }
    }

    private fun IngredientDTO.toDomain(): IngredientDomain {
        return IngredientDomain(
            id = this.id,
            name = this.name,
            description = this.description,
            products = emptyList(), // Products list is complex, usually populated by UseCases if needed
            image = this.image,
            clicks = this.clicks
        )
    }

    private fun IngredientEntity.toDomain(): IngredientDomain {
        return IngredientDomain(
            id = this.id,
            name = this.name,
            description = this.description,
            products = emptyList(),
            image = this.image,
            clicks = this.clicks
        )
    }
}