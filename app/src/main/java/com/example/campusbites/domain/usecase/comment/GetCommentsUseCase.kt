package com.example.campusbites.domain.usecase.comment

import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.repository.CommentRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow // Importa Flow

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(restaurantId: String): Flow<List<CommentDomain>> {
        return commentRepository.getComments(restaurantId)
    }
}