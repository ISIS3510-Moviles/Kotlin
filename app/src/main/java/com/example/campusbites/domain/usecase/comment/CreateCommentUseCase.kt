package com.example.campusbites.domain.usecase.comment

import com.example.campusbites.data.dto.CommentDTO
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.repository.CommentRepository
import jakarta.inject.Inject

class CreateCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(commentDomain: CommentDomain): CommentDomain {
        // Domain → DTO
        val dto = CommentDTO(
            id          = commentDomain.id,
            message     = commentDomain.message,
            rating      = commentDomain.rating,
            likes       = commentDomain.likes ?: 0,
            isVisible   = commentDomain.isVisible,
            photos      = commentDomain.photo ?: emptyList(),
            restaurantId= commentDomain.restaurantDomain?.id,
            authorId    = commentDomain.author.id,
            datetime    = commentDomain.datetime
        )

        val created = commentRepository.createComment(dto)

        // Reusa la entidad de dominio recibida (o mapea la respuesta si el back añade datos)
        return commentDomain.copy(id = created.id)
    }
}