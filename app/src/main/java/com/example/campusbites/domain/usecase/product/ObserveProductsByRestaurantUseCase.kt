package com.example.campusbites.domain.usecase.product

import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveProductsByRestaurantUseCase @Inject constructor(
    private val homeDataRepository: HomeDataRepository, // Para el caché
    private val productRepository: ProductRepository // Para la lógica de obtener de API (si es necesario aquí)
    // Considerar si se necesita el DAO de pending actions aquí o si el repo ya lo maneja
) {
    operator fun invoke(restaurantId: String): Flow<List<ProductDomain>> {
        // Combina datos del caché de HomeDataRepository (que es la fuente principal para UI)
        // Se asume que ProductRepositoryImpl ya maneja la actualización de HomeDataRepository
        // y la sincronización de acciones pendientes.
        // Este UseCase entonces solo lee del caché principal.
        return homeDataRepository.allProductsFlow.map { allProducts ->
            allProducts.filter { it.restaurantId == restaurantId }
            // Aquí se podría añadir lógica para superponer productos pendientes si fuera necesario
            // y ProductRepository no lo hiciera ya al actualizar HomeDataRepository.
        }
    }
}