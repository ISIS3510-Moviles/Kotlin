package com.example.campusbites.domain.usecase.comment

import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.PhotoDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.ReportDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.CommentRepository
import com.example.campusbites.data.dto.CommentDTO
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase
) {
    suspend operator fun invoke(restaurantId: String): List<CommentDomain> {
        val commentDTOs = commentRepository.getComments(restaurantId)

        return commentDTOs.map {
            CommentDomain(id = it.id,
                datetime = it.datetime,
                message = it.message,
                rating = it.rating,
                likes = it.likes,
                photo = it.photos,
                isVisible = it.isVisible,
                author = getUserByIdUseCase(it.authorId),
                responses = null,
                responseTo = null,
                reports = null,
                productDomain = null,
                restaurantDomain = it.restaurantId?.let { it1 -> getRestaurantByIdUseCase(it1) })
        }
    }

}
