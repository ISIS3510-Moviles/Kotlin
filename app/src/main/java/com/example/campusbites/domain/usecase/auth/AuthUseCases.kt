package com.example.campusbites.domain.usecase.auth

import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetCurrentUserFlowUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserDomain?> {
        return authRepository.currentUser
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): UserDomain? {
        return authRepository.getCurrentUser()
    }
}

class PerformCheckOrCreateUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String, userName: String, userEmail: String): UserDomain {
        return authRepository.performCheckOrCreateUser(userId, userName, userEmail)
    }
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.signOut()
    }
}

class UpdateCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(updatedUser: UserDomain?) {
        authRepository.updateCurrentUser(updatedUser)
    }
}