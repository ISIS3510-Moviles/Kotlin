package com.example.campusbites.domain.model

import java.time.LocalDateTime

data class DiscountDomain(
    val id: Int,
    val name: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val description: String,
    val percentage: Double,
    val discountedPrice: Int,
    val isAvailable: Boolean,
    val discountedProductDomains: List<ProductDomain>  = emptyList()
)
