package com.example.campusbites.domain.usecase.alert

import com.example.campusbites.domain.repository.AlertRepository
import javax.inject.Inject

class UpdateAlertUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(alertId: String, newVotes: Int) {
        repository.updateAlertVotes(alertId, newVotes)
    }

}