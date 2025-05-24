package com.example.campusbites.domain.usecase.tag

import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.repository.DietaryTagRepository
import javax.inject.Inject

class GetDietaryTagsUseCase @Inject constructor(
    private val repository: DietaryTagRepository
) {
    suspend operator fun invoke(): List<DietaryTagDomain> {
        return repository.getDietaryTags().map { dto ->
            DietaryTagDomain(
                id = dto.id,
                name = dto.name,
                description = dto.description
            )
        }
    }
}