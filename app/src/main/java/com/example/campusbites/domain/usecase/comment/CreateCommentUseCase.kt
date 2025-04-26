package com.example.campusbites.domain.usecase.comment

import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.repository.CommentRepository
import javax.inject.Inject

class CreateCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(commentDomain: CommentDomain): CommentDomain {
        return commentRepository.createComment(commentDomain)
    }
}