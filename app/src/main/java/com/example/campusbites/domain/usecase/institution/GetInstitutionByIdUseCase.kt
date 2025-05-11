package com.example.campusbites.domain.usecase.institution

import com.example.campusbites.domain.model.InstitutionDomain
import com.example.campusbites.domain.repository.InstitutionRepository
import javax.inject.Inject // Cambiado de jakarta.inject.Inject

class GetInstitutionByIdUseCase @Inject constructor(
    private val repository: InstitutionRepository
) {
    suspend operator fun invoke(id: String): InstitutionDomain? {
        val institutionDTO = repository.getInstitutionById(id)
        return institutionDTO?.let { dto ->
            InstitutionDomain(
                id = dto.id,
                name = dto.name,
                description = dto.description,
            )
        }
    }
}