package com.example.campusbites.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location
}