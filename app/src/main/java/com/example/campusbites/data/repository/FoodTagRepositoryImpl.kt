package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.local.dao.FoodTagDao
import com.example.campusbites.data.local.entity.FoodTagEntity
import com.example.campusbites.data.mapper.TagMapper
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.repository.FoodTagRepository
import javax.inject.Inject

class FoodTagRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val foodTagDao: FoodTagDao,
    private val tagMapper: TagMapper
) : FoodTagRepository {

    override suspend fun getFoodTags(): List<FoodTagDomain> {
        return try {
            val dtos = apiService.getFoodTags()
            val entities = dtos.map { FoodTagEntity(it.id, it.name, it.description) }
            foodTagDao.insertAll(entities)
            dtos.map { tagMapper.mapFoodTagDtoToDomain(it) }
        } catch (e: Exception) {
            Log.w("FoodTagRepository", "API call failed for getFoodTags, fetching from local DB. Error: ${e.message}")
            foodTagDao.getAllOnce().map { FoodTagDomain(it.id, it.name, it.description) }
        }
    }

    override suspend fun getFoodTagById(id: String): FoodTagDomain? {
        return try {
            val dto = apiService.getFoodTagById(id)
            tagMapper.mapFoodTagDtoToDomain(dto)
        } catch (e: Exception) {
            Log.w("FoodTagRepository", "API call failed for getFoodTagById($id), fetching from local DB. Error: ${e.message}")
            foodTagDao.getById(id)?.let { FoodTagDomain(it.id, it.name, it.description) }
        }
    }
}