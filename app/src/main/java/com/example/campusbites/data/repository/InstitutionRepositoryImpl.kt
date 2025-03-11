package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.InstitutionDTO
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.InstitutionRepository
import com.example.campusbites.domain.repository.ProductRepository
import jakarta.inject.Inject

class InstitutionRepositoryImpl @Inject constructor(
    private val apiService: ApiService
): InstitutionRepository {

    override suspend fun getInstitutionById(id: String): InstitutionDTO {
        return apiService.getInstitutionById(id)
    }

}