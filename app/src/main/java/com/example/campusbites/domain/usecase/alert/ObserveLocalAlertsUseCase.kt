package com.example.campusbites.domain.usecase.alert

import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLocalAlertsUseCase @Inject constructor(
    private val repository: AlertRepository
) {
    operator fun invoke(): Flow<List<AlertDomain>> {
        return repository.getLocalAlertsFlow()
    }
}