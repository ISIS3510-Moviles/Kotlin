package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.DietaryTagDTO
import com.example.campusbites.domain.model.DietaryTagDomain

interface DietaryTagRepository {
    suspend fun getDietaryTags(): List<DietaryTagDTO>
    suspend fun getDietaryTagById(id: String): DietaryTagDTO
}