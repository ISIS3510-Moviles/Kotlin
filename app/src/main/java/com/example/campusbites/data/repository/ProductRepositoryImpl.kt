package com.example.campusbites.data.repository

import android.util.Log
import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.data.dto.ProductDTO
import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.data.local.dao.PendingProductActionDao
import com.example.campusbites.data.local.entity.PendingProductActionEntity
import com.example.campusbites.data.mapper.ProductMapper
import com.example.campusbites.data.network.ApiService
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository // Importar HomeDataRepository
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val productMapper: ProductMapper,
    private val pendingProductActionDao: PendingProductActionDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val homeDataRepository: HomeDataRepository, // Inyectar HomeDataRepository
    private val applicationScope: CoroutineScope
) : ProductRepository {

    private val TAG = "ProductRepositoryImpl"

    init {
        applicationScope.launch {
            connectivityMonitor.isNetworkAvailable.collect { isOnline ->
                if (isOnline) {
                    Log.d(TAG, "Network is back online. Flushing pending product actions.")
                    flushPendingProductActions()
                }
            }
        }
    }

    // Función helper para refrescar la caché de productos en HomeDataRepository
    private suspend fun refreshProductsCacheForRestaurant(restaurantId: String) {
        try {
            Log.d(TAG, "Refreshing products cache for restaurant: $restaurantId")
            val allProductsFromApi = apiService.getProducts() // Obtener todos los productos de la API
            // Filtrar y mapear solo los productos del restaurante afectado
            val restaurantProductsDomain = allProductsFromApi
                .filter { it.restaurant_id == restaurantId }
                .map { productMapper.mapDtoToDomain(it) }

            // Obtener la lista actual de todos los productos del HomeDataRepository
            val currentAllCachedProducts = homeDataRepository.allProductsFlow.first().toMutableList()
            // Eliminar los productos antiguos de este restaurante del caché
            currentAllCachedProducts.removeAll { it.restaurantId == restaurantId }
            // Añadir los productos actualizados/nuevos de este restaurante
            currentAllCachedProducts.addAll(restaurantProductsDomain)
            // Guardar la lista completa actualizada en HomeDataRepository
            homeDataRepository.saveAllProducts(currentAllCachedProducts)
            Log.d(TAG, "Products cache updated for restaurant $restaurantId. Total products in cache: ${currentAllCachedProducts.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing products cache for restaurant $restaurantId: ${e.message}", e)
            // Considerar si se debe notificar al usuario o reintentar.
            // Por ahora, solo logueamos el error. La UI seguirá mostrando el caché antiguo.
        }
    }


    override suspend fun getProducts(): List<ProductDTO> {
        // Esta función se usa en HomeViewModel para la carga inicial.
        // Debería actualizar el HomeDataRepository también.
        return try {
            val productsDto = apiService.getProducts()
            val productsDomain = productsDto.map { productMapper.mapDtoToDomain(it) }
            homeDataRepository.saveAllProducts(productsDomain) // Actualizar caché global
            productsDto
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProducts: ${e.message}", e)
            // Devolver desde el caché si la API falla
            homeDataRepository.allProductsFlow.first().map { domain ->
                // Necesitamos reconvertir a DTO si la firma del método lo exige,
                // o cambiar la firma para que devuelva List<ProductDomain>
                // Por ahora, asumimos una conversión simple o que la UI puede manejar ProductDomain.
                // Esta parte es compleja si la firma DEBE ser List<ProductDTO>.
                // Mejor sería que los UseCases que usan esto esperen ProductDomain.
                ProductDTO(domain.id, domain.name, domain.description, domain.price, domain.photo, domain.restaurantId, domain.rating, domain.ingredientsIds, domain.discountsIds, domain.commentsIds, domain.foodTags.map{it.id}, domain.dietaryTags.map{it.id})
            }
        }
    }

    override suspend fun getProductById(id: String): ProductDTO {
        // Para obtener un solo producto, generalmente no se refresca todo el caché.
        // Se devuelve directamente y se asume que la UI que lo usa es para detalle.
        return apiService.getProductById(id)
    }

    override suspend fun getProductsByRestaurant(id: String): List<ProductDTO> {
        // Similar a getProducts, pero filtrado. Refresca el caché global.
        return try {
            val allProductsDto = apiService.getProducts()
            val restaurantProductsDto = allProductsDto.filter { it.restaurant_id == id }

            // Actualizar el caché global con todos los productos, no solo los del restaurante
            val allProductsDomain = allProductsDto.map { productMapper.mapDtoToDomain(it) }
            homeDataRepository.saveAllProducts(allProductsDomain)

            restaurantProductsDto
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProductsByRestaurant for $id: ${e.message}", e)
            val cachedRestaurantProducts = homeDataRepository.allProductsFlow.first()
                .filter { it.restaurantId == id }
                .map { domain -> ProductDTO(domain.id, domain.name, domain.description, domain.price, domain.photo, domain.restaurantId, domain.rating, domain.ingredientsIds, domain.discountsIds, domain.commentsIds, domain.foodTags.map{it.id}, domain.dietaryTags.map{it.id}) }
            cachedRestaurantProducts
        }
    }

    override suspend fun searchProducts(query: String): List<ProductDTO> {
        return try {
            val response = apiService.searchProducts(query)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                Log.e(TAG, "Error HTTP searching products: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching products", e)
            emptyList()
        }
    }

    override suspend fun createProduct(product: CreateProductDTO): ProductDomain {
        return withContext(Dispatchers.IO) {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                try {
                    Log.d(TAG, "Attempting to create product online: ${product.name}")
                    val response = apiService.createProduct(product)
                    if (response.isSuccessful && response.body() != null) {
                        val createdDto = response.body()!!
                        Log.d(TAG, "Product ${createdDto.id} created successfully online.")
                        val createdDomain = productMapper.mapDtoToDomain(createdDto)
                        refreshProductsCacheForRestaurant(createdDomain.restaurantId) // Refrescar caché
                        createdDomain
                    } else {
                        Log.e(TAG, "Failed to create product online. Code: ${response.code()}, Message: ${response.message()}, Body: ${response.errorBody()?.string()}")
                        throw RuntimeException("Failed to create product online. Server responded with ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating product online, queueing. Product: ${product.name}, Error: ${e.message}", e)
                    queueProductAction(product, null, "CREATE")
                    throw e
                }
            } else {
                Log.d(TAG, "Offline. Queueing product creation: ${product.name}")
                queueProductAction(product, null, "CREATE")
                throw RuntimeException("Offline: Product creation queued.")
            }
        }
    }

    override suspend fun updateProduct(productId: String, productUpdate: UpdateProductDTO): ProductDomain {
        return withContext(Dispatchers.IO) {
            if (connectivityMonitor.isNetworkAvailable.first()) {
                try {
                    Log.d(TAG, "Attempting to update product $productId online.")
                    val response = apiService.updateProduct(productId, productUpdate)
                    if (response.isSuccessful && response.body() == true) {
                        Log.d(TAG, "Product $productId updated successfully online. Fetching updated product details...")
                        val updatedProductDTO = apiService.getProductById(productId)
                        val updatedDomain = productMapper.mapDtoToDomain(updatedProductDTO)
                        refreshProductsCacheForRestaurant(updatedDomain.restaurantId) // Refrescar caché
                        updatedDomain
                    } else if (response.isSuccessful && response.body() == false) {
                        Log.w(TAG, "Product $productId update acknowledged by server but indicated no change (server returned false).")
                        val currentProductDTO = apiService.getProductById(productId)
                        val currentDomain = productMapper.mapDtoToDomain(currentProductDTO)
                        // Opcional: refrescar caché incluso si no hubo cambios, por si acaso
                        // refreshProductsCacheForRestaurant(currentDomain.restaurantId)
                        currentDomain
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Failed to update product $productId online. Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody")
                        throw RuntimeException("Failed to update product $productId online. Server responded with ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating product $productId online, queueing. Error: ${e.message}", e)
                    queueProductAction(null, productUpdate, "UPDATE", productId)
                    throw e
                }
            } else {
                Log.d(TAG, "Offline. Queueing product update for $productId.")
                queueProductAction(null, productUpdate, "UPDATE", productId)
                throw RuntimeException("Offline: Product update queued.")
            }
        }
    }

    override suspend fun deleteProduct(productId: String): Boolean {
        return withContext(Dispatchers.IO) {
            // Necesitamos el restaurantId para refrescar el caché.
            // Intentamos obtenerlo del producto existente antes de borrarlo.
            var restaurantIdToRefresh: String? = null
            try {
                val productToDelete = homeDataRepository.allProductsFlow.first().find { it.id == productId }
                restaurantIdToRefresh = productToDelete?.restaurantId
            } catch (e: Exception) {
                Log.w(TAG, "Could not get restaurantId for product $productId before deletion. Cache refresh might be incomplete.", e)
            }

            if (connectivityMonitor.isNetworkAvailable.first()) {
                try {
                    Log.d(TAG, "Attempting to delete product $productId online.")
                    val response = apiService.deleteProduct(productId)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Product $productId deleted successfully online.")
                        restaurantIdToRefresh?.let { refreshProductsCacheForRestaurant(it) } // Refrescar caché
                        true
                    } else {
                        Log.e(TAG, "Failed to delete product $productId online. Code: ${response.code()}, Message: ${response.message()}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting product $productId online, queueing. Error: ${e.message}", e)
                    queueProductAction(null, null, "DELETE", productId, restaurantIdToRefresh) // Pasar restaurantId para el flush
                    throw e
                }
            } else {
                Log.d(TAG, "Offline. Queueing product deletion for $productId.")
                queueProductAction(null, null, "DELETE", productId, restaurantIdToRefresh) // Pasar restaurantId para el flush
                throw RuntimeException("Offline: Product deletion queued.")
            }
        }
    }

    private suspend fun queueProductAction(
        createDto: CreateProductDTO?,
        updateDto: UpdateProductDTO?,
        actionType: String,
        productIdForUpdateOrDelete: String? = null,
        restaurantIdForCacheRefresh: String? = null // Nuevo parámetro
    ) {
        val entity = PendingProductActionEntity(
            actionType = actionType,
            productId = productIdForUpdateOrDelete ?: (if (actionType == "CREATE") java.util.UUID.randomUUID().toString() else null),
            name = createDto?.name ?: updateDto?.name,
            description = createDto?.description ?: updateDto?.description,
            price = createDto?.price ?: updateDto?.price,
            photo = createDto?.photo ?: updateDto?.photo,
            restaurantId = createDto?.restaurant_id ?: restaurantIdForCacheRefresh, // Usar el ID de restaurante para el flush
            ingredientsIds = createDto?.ingredientsIds ?: updateDto?.ingredientsIds,
            foodTagsIds = createDto?.foodTagsIds ?: updateDto?.foodTagsIds,
            dietaryTagsIds = createDto?.dietaryTagsIds ?: updateDto?.dietaryTagsIds
        )
        pendingProductActionDao.insertAction(entity)
        Log.i(TAG, "Queued product action: $actionType for product ID: ${entity.productId ?: "new (local ID ${entity.id})"}. Restaurant for refresh: ${entity.restaurantId}")
    }

    private suspend fun flushPendingProductActions() {
        val pendingActions = pendingProductActionDao.getAllActions()
        if (pendingActions.isEmpty()) {
            Log.d(TAG, "No pending product actions to flush.")
            return
        }
        Log.d(TAG, "Flushing ${pendingActions.size} pending product actions.")

        var anyActionSuccessful = false
        var lastProcessedRestaurantId: String? = null

        for (actionEntity in pendingActions) {
            try {
                when (actionEntity.actionType) {
                    "CREATE" -> {
                        val createDto = CreateProductDTO(
                            name = actionEntity.name ?: "",
                            description = actionEntity.description ?: "",
                            price = actionEntity.price ?: 0f,
                            photo = actionEntity.photo ?: "",
                            restaurant_id = actionEntity.restaurantId ?: "",
                            ingredientsIds = actionEntity.ingredientsIds ?: emptyList(),
                            foodTagsIds = actionEntity.foodTagsIds ?: emptyList(),
                            dietaryTagsIds = actionEntity.dietaryTagsIds ?: emptyList()
                        )
                        if (createDto.restaurant_id.isBlank()) {
                            Log.e(TAG, "Cannot flush CREATE action for ${actionEntity.name}, restaurantId is missing. Action ID: ${actionEntity.id}")
                            continue
                        }
                        apiService.createProduct(createDto).also {
                            if (!it.isSuccessful) throw RuntimeException("Flush CREATE failed with code ${it.code()}")
                            Log.i(TAG, "Flushed CREATE for ${actionEntity.name}, new backend ID: ${it.body()?.id}")
                            lastProcessedRestaurantId = createDto.restaurant_id
                        }
                    }
                    "UPDATE" -> {
                        if (actionEntity.productId == null) {
                            Log.e(TAG, "Cannot flush UPDATE action, productId is missing. Action ID: ${actionEntity.id}")
                            continue
                        }
                        val updateDto = UpdateProductDTO(
                            name = actionEntity.name,
                            description = actionEntity.description,
                            price = actionEntity.price,
                            photo = actionEntity.photo,
                            ingredientsIds = actionEntity.ingredientsIds,
                            foodTagsIds = actionEntity.foodTagsIds,
                            dietaryTagsIds = actionEntity.dietaryTagsIds
                        )
                        apiService.updateProduct(actionEntity.productId, updateDto).also {
                            if (!it.isSuccessful || it.body() != true) throw RuntimeException("Flush UPDATE for ${actionEntity.productId} failed with code ${it.code()} or body not true")
                            Log.i(TAG, "Flushed UPDATE for ${actionEntity.productId}")
                            // Obtener el producto para saber su restaurantId
                            val productDto = apiService.getProductById(actionEntity.productId)
                            lastProcessedRestaurantId = productDto.restaurant_id
                        }
                    }
                    "DELETE" -> {
                        if (actionEntity.productId == null) {
                            Log.e(TAG, "Cannot flush DELETE action, productId is missing. Action ID: ${actionEntity.id}")
                            continue
                        }
                        apiService.deleteProduct(actionEntity.productId).also {
                            if (!it.isSuccessful) throw RuntimeException("Flush DELETE for ${actionEntity.productId} failed with code ${it.code()}")
                            Log.i(TAG, "Flushed DELETE for ${actionEntity.productId}")
                            lastProcessedRestaurantId = actionEntity.restaurantId // Usar el restaurantId guardado en la entidad pendiente
                        }
                    }
                }
                pendingProductActionDao.deleteActionById(actionEntity.id)
                anyActionSuccessful = true
                Log.d(TAG, "Successfully processed and removed pending action ID: ${actionEntity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush product action ID ${actionEntity.id} (${actionEntity.actionType}). Error: ${e.message}", e)
                break // Detener el flush si una acción falla, para reintentar más tarde
            }
        }

        // Si alguna acción fue exitosa y tenemos un restaurantId, refrescamos el caché una vez.
        // Si múltiples acciones para diferentes restaurantes se procesaron, idealmente se refrescaría cada uno,
        // o se haría un refresh global. Por simplicidad aquí, refrescamos el último afectado.
        if (anyActionSuccessful && lastProcessedRestaurantId != null) {
            refreshProductsCacheForRestaurant(lastProcessedRestaurantId!!)
        } else if (anyActionSuccessful) {
            // Si fue exitoso pero no tenemos un restaurantId específico (ej. un delete donde no se pudo obtener),
            // podríamos considerar un refresh más genérico o loguear.
            Log.w(TAG, "Pending actions flushed, but lastProcessedRestaurantId is null. Cache may not be fully up-to-date for a specific restaurant.")
            // Podrías forzar un refresh de todos los productos si es necesario:
            // applicationScope.launch { getProducts() } // Esto llamaría a la función que refresca todo el HomeDataRepo
        }
    }
}