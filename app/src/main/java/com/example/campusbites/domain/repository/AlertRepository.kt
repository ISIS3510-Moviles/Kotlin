package com.example.campusbites.domain.repository

import com.example.campusbites.domain.model.AlertDomain

interface AlertRepository {
    suspend fun getAlerts(): List<AlertDomain>
}