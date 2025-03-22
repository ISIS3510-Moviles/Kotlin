package com.example.campusbites.domain.usecase.institution

import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.repository.InstitutionRepository
import jakarta.inject.Inject

class GetInstitutionByIdUseCase @Inject constructor(
    private val repository: InstitutionRepository
) {
    suspend operator fun invoke(id: String): InstitutionDomain {
        val institutionDTO = repository.getInstitutionById(id)
        return InstitutionDomain(
            id = institutionDTO.id,
            name = institutionDTO.name,
            description = institutionDTO.description,
        )
    }
}