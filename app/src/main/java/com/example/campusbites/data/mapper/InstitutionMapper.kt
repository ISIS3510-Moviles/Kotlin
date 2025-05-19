// package com.example.campusbites.data.mapper
package com.example.campusbites.data.mapper

import com.example.campusbites.data.dto.InstitutionDTO
import com.example.campusbites.domain.model.InstitutionDomain
import javax.inject.Inject

class InstitutionMapper @Inject constructor(
    // private val userMapper: UserMapper // Descomentar si mapeas UserDomain completo para members
) {
    fun mapDtoToDomain(dto: InstitutionDTO?): InstitutionDomain? {
        return dto?.let {
            InstitutionDomain(
                id = it.id,
                name = it.name,
                description = it.description,
                members = emptyList(),
                buildings = emptyList()
            )
        }
    }

    fun createDefault(id: String, name: String = "Institución desconocida"): InstitutionDomain {
        return InstitutionDomain(
            id = id,
            name = name,
            description = "",
            members = emptyList(),
            buildings = emptyList()
        )
    }
}