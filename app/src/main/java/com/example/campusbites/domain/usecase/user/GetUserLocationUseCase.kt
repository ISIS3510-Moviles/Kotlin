package com.example.campusbites.domain.usecase.user

import android.location.Location
import com.example.campusbites.domain.repository.LocationRepository
import jakarta.inject.Inject

class GetUserLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Location {
        return locationRepository.getCurrentLocation()
    }
}
