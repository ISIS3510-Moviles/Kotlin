package com.example.campusbites.data.repository

import com.example.campusbites.data.dto.CommentDTO
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.domain.repository.CommentRepository
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : CommentRepository {

    override suspend fun getComments(restaurantId: String): List<CommentDTO> {
        return apiService.getComments().filter { it.restaurantId == restaurantId }
    }

    override suspend fun getAllComments(): List<CommentDTO> {
        return apiService.getComments()
    }

    override suspend fun createComment(commentDTO: CommentDTO): CommentDTO =
        apiService.createComment(commentDTO)
}
