package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.DietaryTagDomain

interface DietaryTagRepository {
    suspend fun getDietaryTags(): List<DietaryTagDomain>
    suspend fun getDietaryTagById(id: String): DietaryTagDomain? // Puede ser null si no se encuentra
}