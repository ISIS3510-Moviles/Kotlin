package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.InstitutionDTO

interface InstitutionRepository {
    suspend fun getInstitutionById(id: String): InstitutionDTO?
}