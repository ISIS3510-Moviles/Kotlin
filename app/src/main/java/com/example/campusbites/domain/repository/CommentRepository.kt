package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.CommentDomain
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getComments(restaurantId: String): Flow<List<CommentDomain>>
    fun getAllComments(): Flow<List<CommentDomain>>
    suspend fun createComment(comment: CommentDomain): CommentDomain
}