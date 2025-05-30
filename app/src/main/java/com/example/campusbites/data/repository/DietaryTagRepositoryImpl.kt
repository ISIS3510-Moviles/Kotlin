package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.local.dao.DietaryTagDao
import com.example.campusbites.data.local.entity.DietaryTagEntity
import com.example.campusbites.data.mapper.TagMapper
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.repository.DietaryTagRepository
import javax.inject.Inject

class DietaryTagRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val dietaryTagDao: DietaryTagDao,
    private val tagMapper: TagMapper
) : DietaryTagRepository {

    override suspend fun getDietaryTags(): List<DietaryTagDomain> {
        return try {
            val dtos = apiService.getDietaryTags()
            val entities = dtos.map { DietaryTagEntity(it.id, it.name, it.description) }
            dietaryTagDao.insertAll(entities)
            dtos.map { tagMapper.mapDietaryTagDtoToDomain(it) }
        } catch (e: Exception) {
            Log.w("DietaryTagRepository", "API call failed for getDietaryTags, fetching from local DB. Error: ${e.message}")
            dietaryTagDao.getAllOnce().map { DietaryTagDomain(it.id, it.name, it.description) }
        }
    }

    override suspend fun getDietaryTagById(id: String): DietaryTagDomain? {
        return try {
            val dto = apiService.getDietaryTagById(id)
            tagMapper.mapDietaryTagDtoToDomain(dto)
        } catch (e: Exception) {
            Log.w("DietaryTagRepository", "API call failed for getDietaryTagById($id), fetching from local DB. Error: ${e.message}")
            dietaryTagDao.getById(id)?.let { DietaryTagDomain(it.id, it.name, it.description) }
        }
    }
}