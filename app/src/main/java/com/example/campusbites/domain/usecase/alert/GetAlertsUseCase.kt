package com.example.campusbites.domain.usecase.alert

import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.repository.AlertRepository
import javax.inject.Inject

class GetAlertsUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    suspend operator fun invoke(): List<AlertDomain> {
        return repository.getAlerts()
    }
}