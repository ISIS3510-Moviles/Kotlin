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
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val productMapper: ProductMapper,
    private val pendingProductActionDao: PendingProductActionDao,
    private val connectivityMonitor: ConnectivityMonitor,
    private val homeDataRepository: HomeDataRepository,
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

    private suspend fun refreshProductsCacheForRestaurant(restaurantId: String) {
        try {
            Log.d(TAG, "Refreshing products cache for restaurant: $restaurantId")
            val allProductsFromApi = apiService.getProducts()
            val restaurantProductsDomain = allProductsFromApi
                .filter { it.restaurant_id == restaurantId }
                .map { productMapper.mapDtoToDomain(it) }

            val currentAllCachedProducts = homeDataRepository.allProductsFlow.first().toMutableList()
            currentAllCachedProducts.removeAll { it.restaurantId == restaurantId }
            currentAllCachedProducts.addAll(restaurantProductsDomain)
            homeDataRepository.saveAllProducts(currentAllCachedProducts)
            Log.d(TAG, "Products cache updated for restaurant $restaurantId. Total products in cache: ${currentAllCachedProducts.size}")

        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing products cache for restaurant $restaurantId: ${e.message}", e)
        }
    }

    override suspend fun getProducts(): List<ProductDomain> {
        return try {
            val productsDto = apiService.getProducts()
            val productsDomain = productsDto.map { productMapper.mapDtoToDomain(it) }
            homeDataRepository.saveAllProducts(productsDomain)
            productsDomain
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProducts: ${e.message}", e)
            homeDataRepository.allProductsFlow.first()
        }
    }

    override suspend fun getProductById(id: String): ProductDomain? {
        try {
            val productDto = apiService.getProductById(id)
            val productDomain = productMapper.mapDtoToDomain(productDto)

            val currentProducts = homeDataRepository.allProductsFlow.first().toMutableList()
            val index = currentProducts.indexOfFirst { it.id == productDomain.id }
            if (index != -1) {
                currentProducts[index] = productDomain
            } else {
                currentProducts.add(productDomain)
            }
            homeDataRepository.saveAllProducts(currentProducts)
            return productDomain
        } catch (e: Exception) {
            Log.w(TAG, "API failed for getProductById($id), trying cache. Error: ${e.message}")
            val cachedProduct = homeDataRepository.allProductsFlow.first().find { it.id == id }
            if (cachedProduct != null) {
                Log.d(TAG, "Found product $id in HomeDataRepository cache.")
                return cachedProduct
            } else {
                Log.e(TAG, "Product $id not found in API or cache.")
                return null
            }
        }
    }

    override suspend fun getProductsByRestaurant(id: String): List<ProductDomain> {
        return try {
            val allProductsDto = apiService.getProducts()
            val restaurantProductsDto = allProductsDto.filter { it.restaurant_id == id }
            val restaurantProductsDomain = restaurantProductsDto.map { productMapper.mapDtoToDomain(it) }

            val allProductsDomain = allProductsDto.map { productMapper.mapDtoToDomain(it) }
            homeDataRepository.saveAllProducts(allProductsDomain)

            restaurantProductsDomain
        } catch (e: Exception) {
            Log.e(TAG, "Error in getProductsByRestaurant for $id: ${e.message}", e)
            homeDataRepository.allProductsFlow.first().filter { it.restaurantId == id }
        }
    }

    override suspend fun searchProducts(query: String): List<ProductDomain> {
        return try {
            val response = apiService.searchProducts(query)
            if (response.isSuccessful) {
                response.body()?.map { productMapper.mapDtoToDomain(it) } ?: emptyList()
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
                        refreshProductsCacheForRestaurant(createdDomain.restaurantId)
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
                // Simular un ProductDomain para la UI (con ID temporal si es necesario)
                // o lanzar una excepción específica que la UI pueda interpretar como "encolado".
                // Por ahora, lanzaremos una excepción indicando que fue encolado.
                throw RuntimeException("Offline: Product creation queued.")
            }
        }
    }

    override suspend fun updateProduct(productId: String, productUpdate: UpdateProductDTO): ProductDomain {
        return withContext(Dispatchers.IO) {
            // Primero, intentar obtener el producto actual (ya sea de API o caché) para obtener el restaurantId
            val currentProduct = getProductById(productId) // Esto ahora devuelve ProductDomain?
            val restaurantIdForCache = currentProduct?.restaurantId

            if (connectivityMonitor.isNetworkAvailable.first()) {
                try {
                    Log.d(TAG, "Attempting to update product $productId online.")
                    val response = apiService.updateProduct(productId, productUpdate)
                    if (response.isSuccessful && response.body() == true) {
                        Log.d(TAG, "Product $productId updated successfully online. Fetching updated product details...")
                        // Volver a obtener el producto para tener la versión más reciente del servidor
                        val updatedProductFromServer = getProductById(productId) // Esto ya actualiza el caché
                        if (updatedProductFromServer == null) {
                            throw RuntimeException("Product $productId not found after update.")
                        }
                        // El refresh del caché ya se hizo dentro de getProductById
                        updatedProductFromServer
                    } else if (response.isSuccessful && response.body() == false) {
                        Log.w(TAG, "Product $productId update acknowledged by server but indicated no change (server returned false).")
                        currentProduct ?: throw RuntimeException("Product $productId not found and update returned false.")
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Failed to update product $productId online. Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody")
                        throw RuntimeException("Failed to update product $productId online. Server responded with ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating product $productId online, queueing. Error: ${e.message}", e)
                    queueProductAction(null, productUpdate, "UPDATE", productId, restaurantIdForCache)
                    throw e
                }
            } else {
                Log.d(TAG, "Offline. Queueing product update for $productId.")
                queueProductAction(null, productUpdate, "UPDATE", productId, restaurantIdForCache)
                throw RuntimeException("Offline: Product update queued.")
            }
        }
    }

    override suspend fun deleteProduct(productId: String): Boolean {
        return withContext(Dispatchers.IO) {
            var restaurantIdToRefresh: String? = null
            try {
                // Obtener el restaurantId del producto ANTES de intentar borrarlo
                // Usar getProductById que ya tiene la lógica de API/caché.
                val productToDelete = getProductById(productId)
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
                        // Eliminar del caché local
                        val currentProducts = homeDataRepository.allProductsFlow.first().toMutableList()
                        currentProducts.removeAll { it.id == productId }
                        homeDataRepository.saveAllProducts(currentProducts)
                        // No es necesario refreshProductsCacheForRestaurant si actualizamos el caché global así.
                        true
                    } else {
                        Log.e(TAG, "Failed to delete product $productId online. Code: ${response.code()}, Message: ${response.message()}")
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting product $productId online, queueing. Error: ${e.message}", e)
                    queueProductAction(null, null, "DELETE", productId, restaurantIdToRefresh)
                    throw e
                }
            } else {
                Log.d(TAG, "Offline. Queueing product deletion for $productId.")
                queueProductAction(null, null, "DELETE", productId, restaurantIdToRefresh)
                throw RuntimeException("Offline: Product deletion queued.")
            }
        }
    }

    private suspend fun queueProductAction(
        createDto: CreateProductDTO?,
        updateDto: UpdateProductDTO?,
        actionType: String,
        productIdForUpdateOrDelete: String? = null,
        restaurantIdForCacheRefresh: String? = null
    ) {
        val entity = PendingProductActionEntity(
            actionType = actionType,
            productId = productIdForUpdateOrDelete ?: (if (actionType == "CREATE") java.util.UUID.randomUUID().toString() else null),
            name = createDto?.name ?: updateDto?.name,
            description = createDto?.description ?: updateDto?.description,
            price = createDto?.price ?: updateDto?.price,
            photo = createDto?.photo ?: updateDto?.photo,
            restaurantId = createDto?.restaurant_id ?: restaurantIdForCacheRefresh,
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
        val restaurantIdsToRefresh = mutableSetOf<String>()

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
                            actionEntity.restaurantId?.let { id -> restaurantIdsToRefresh.add(id) }
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
                        apiService.updateProduct(actionEntity.productId!!, updateDto).also {
                            if (!it.isSuccessful || it.body() != true) throw RuntimeException("Flush UPDATE for ${actionEntity.productId} failed with code ${it.code()} or body not true")
                            Log.i(TAG, "Flushed UPDATE for ${actionEntity.productId}")
                            actionEntity.restaurantId?.let { id -> restaurantIdsToRefresh.add(id) }
                        }
                    }
                    "DELETE" -> {
                        if (actionEntity.productId == null) {
                            Log.e(TAG, "Cannot flush DELETE action, productId is missing. Action ID: ${actionEntity.id}")
                            continue
                        }
                        apiService.deleteProduct(actionEntity.productId!!).also {
                            if (!it.isSuccessful) throw RuntimeException("Flush DELETE for ${actionEntity.productId} failed with code ${it.code()}")
                            Log.i(TAG, "Flushed DELETE for ${actionEntity.productId}")
                            actionEntity.restaurantId?.let { id -> restaurantIdsToRefresh.add(id) }
                        }
                    }
                }
                pendingProductActionDao.deleteActionById(actionEntity.id)
                anyActionSuccessful = true
                Log.d(TAG, "Successfully processed and removed pending action ID: ${actionEntity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush product action ID ${actionEntity.id} (${actionEntity.actionType}). Error: ${e.message}", e)
                break
            }
        }

        if (anyActionSuccessful) {
            // Refrescar el caché para todos los restaurantes afectados una vez
            // También se podría forzar un refresh de todos los productos si es más simple
            applicationScope.launch {
                Log.d(TAG, "Flushing complete. Refreshing all products cache due to successful actions.")
                try {
                    val allProductsDto = apiService.getProducts()
                    val allProductsDomain = allProductsDto.map { productMapper.mapDtoToDomain(it) }
                    homeDataRepository.saveAllProducts(allProductsDomain)
                    Log.i(TAG, "Global products cache refreshed after flushing actions.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing global products cache after flush: ${e.message}", e)
                }
            }
        }
    }
}