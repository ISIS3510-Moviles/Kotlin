package com.example.campusbites.domain.repository

import com.example.campusbites.data.dto.CommentDTO

interface CommentRepository {
    suspend fun getComments(restaurantId: String): List<CommentDTO>
    suspend fun getAllComments(): List<CommentDTO>
    suspend fun createComment(commentDTO: CommentDTO): CommentDTO
}