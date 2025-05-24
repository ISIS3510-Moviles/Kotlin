package com.example.campusbites.domain.usecase.user

import com.example.campusbites.domain.repository.UserRepository
import com.example.campusbites.data.dto.UserUpdateDTO
import javax.inject.Inject

class UpdateUserRoleUseCase @Inject constructor(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(userId: String, newRole: String): Boolean {
        val userUpdateDTO = UserUpdateDTO(
            id = userId,
            role = newRole
        )
        return userRepository.updateUser(userId, userUpdateDTO)
    }
}