package com.example.campusbites.domain.usecase.tag

import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.repository.DietaryTagRepository
import javax.inject.Inject

class GetDietaryTagByIdUseCase @Inject constructor(
    private val repository: DietaryTagRepository
) {
    suspend operator fun invoke(id: String): DietaryTagDomain? { // Devuelve nullable
        return repository.getDietaryTagById(id)
    }
}