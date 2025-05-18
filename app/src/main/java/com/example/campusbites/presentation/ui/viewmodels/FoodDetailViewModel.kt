package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.local.realm.PendingFavoriteActionLocalDataSource
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.repository.AuthRepository
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val homeDataRepository: HomeDataRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val pendingFavoriteActionDataSource: PendingFavoriteActionLocalDataSource,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _product = MutableStateFlow<ProductDomain?>(null)
    val product: StateFlow<ProductDomain?> = _product

    private val _ingredients = MutableStateFlow<List<IngredientDomain>>(emptyList())
    val ingredients: StateFlow<List<IngredientDomain>> = _ingredients

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Asumimos online inicialmente, se corregirá rápido.
        )

    init {
        viewModelScope.launch {
            isNetworkAvailable
                .filter { it }
                .distinctUntilChanged()
                .collect {
                    Log.d("FoodDetailVM", "Network is available. Flushing pending favorite actions.")
                    flushPendingFavoriteActions()
                }
        }
    }

    fun loadFoodDetail(foodId: String) {
        viewModelScope.launch {
            val cachedProduct = homeDataRepository.allProductsFlow.firstOrNull()?.find { it.id == foodId }
            if (cachedProduct != null) {
                _product.value = cachedProduct
                val allIngredients = homeDataRepository.allIngredientsFlow.firstOrNull() ?: emptyList()
                _ingredients.value = allIngredients.filter { ingredient ->
                    cachedProduct.ingredientsIds.contains(ingredient.id)
                }
                Log.d("FoodDetailVM", "Loaded product $foodId from cache.")
            }

            if (isNetworkAvailable.value || cachedProduct == null) {
                try {
                    Log.d("FoodDetailVM", "Attempting to fetch food detail from server for $foodId. Network: ${isNetworkAvailable.value}")
                    fetchFoodDetailFromServer(foodId)
                } catch (e: Exception) {
                    Log.e("FoodDetailVM", "Error fetching food detail from server for $foodId: ${e.message}", e)
                    if (cachedProduct == null) {
                        _uiEvent.emit(UiEvent.ShowError("Failed to load product details. Please check your connection."))
                    }
                }
            } else if (cachedProduct == null) {
                Log.w("FoodDetailVM", "Offline and no cached data for product $foodId.")
                _uiEvent.emit(UiEvent.ShowError("You are offline. Product details cannot be loaded."))
            }
        }
    }

    private suspend fun fetchFoodDetailFromServer(foodId: String) {
        // getProductByIdUseCase podría devolver null o lanzar excepción si no se encuentra.
        // Asumimos que lanza excepción en caso de error de red/servidor, y devuelve null si no existe.
        val loadedProduct = getProductByIdUseCase(foodId) // Podría ser null si el producto no existe en el backend
        _product.value = loadedProduct // Actualiza el StateFlow, puede ser null

        if (loadedProduct != null) {
            Log.d("FoodDetailVM", "Successfully fetched product $foodId from server.")
            // getIngredientsUseCase podría devolver lista vacía o lanzar excepción.
            val allIngredients = try { getIngredientsUseCase() } catch (e: Exception) { emptyList() }
            _ingredients.value = allIngredients.filter { ingredient ->
                loadedProduct.ingredientsIds.contains(ingredient.id)
            }
        } else {
            Log.w("FoodDetailVM", "Product $foodId not found on server or error fetching.")
            // Si cachedProduct era null y fetch también es null, product seguirá siendo null.
            // Si cachedProduct no era null pero fetch sí, product se volverá null.
            // Considerar si se debe emitir un error específico.
            if (_product.value == null) { // Si sigue siendo null después del intento de fetch
                _uiEvent.emit(UiEvent.ShowError("Product details not found."))
            }
        }
    }

    fun handleFavoriteToggleLogic(
        currentUser: UserDomain?, // Hacer explícito que puede ser null desde la UI
        productId: String?,
        productObjectForUi: ProductDomain?, // El objeto producto que la UI tiene, para el optimistic update
        isCurrentlyFavorite: Boolean
    ) {
        viewModelScope.launch {
            if (currentUser == null) {
                Log.e("FoodDetailVM", "Cannot toggle favorite, currentUser is null.")
                _uiEvent.emit(UiEvent.ShowError("User not logged in."))
                return@launch
            }
            if (productId == null) {
                Log.e("FoodDetailVM", "Cannot toggle favorite, productId is null.")
                _uiEvent.emit(UiEvent.ShowError("Product ID is missing."))
                return@launch
            }

            val newFavoriteState = !isCurrentlyFavorite

            // Lógica para preparar la lista de favoritos para el backend/cola
            val targetSavedProducts = currentUser.savedProducts.toMutableList()
            if (newFavoriteState) { // Se quiere agregar
                // Para agregar, necesitamos el objeto ProductDomain.
                // El productObjectForUi es el que la UI tiene, puede ser el de caché.
                // Es importante que este objeto sea el que se guarde si es para el backend.
                // Si el `product.value` del ViewModel es más actual (de fetch), usar ese.
                val productToActuallyAdd = _product.value?.takeIf { it.id == productId } ?: productObjectForUi

                if (productToActuallyAdd != null && productToActuallyAdd.id == productId) {
                    if (targetSavedProducts.none { it.id == productId }) {
                        targetSavedProducts.add(productToActuallyAdd)
                    }
                } else {
                    Log.e("FoodDetailVM", "Product object to add to favorites is null or ID mismatch for $productId.")
                    _uiEvent.emit(UiEvent.ShowMessage("Error: Product details not available to add favorite."))
                    return@launch
                }
            } else { // Se quiere quitar
                targetSavedProducts.removeAll { it.id == productId }
            }
            val userWithTargetFavorites = currentUser.copy(savedProducts = targetSavedProducts)

            if (isNetworkAvailable.value) {
                try {
                    Log.d("FoodDetailVM", "Online. Updating user ${currentUser.id} favorites on server for product $productId to $newFavoriteState")
                    updateUserUseCase(currentUser.id, userWithTargetFavorites)
                    _uiEvent.emit(UiEvent.ShowMessage(if (newFavoriteState) "Agregado a favoritos" else "Eliminado de favoritos"))
                } catch (e: Exception) {
                    Log.e("FoodDetailVM", "Error updating user on server for $productId, queueing: ${e.message}", e)
                    pendingFavoriteActionDataSource.addOrUpdateAction(currentUser.id, productId, newFavoriteState)
                    _uiEvent.emit(UiEvent.ShowMessage("Error de red. Tu cambio de favorito se guardó y se procesará más tarde."))
                }
            } else {
                Log.d("FoodDetailVM", "Offline. Queueing favorite action for user ${currentUser.id}, product $productId to $newFavoriteState")
                pendingFavoriteActionDataSource.addOrUpdateAction(currentUser.id, productId, newFavoriteState)
                _uiEvent.emit(UiEvent.ShowMessage("Estás offline. Tu cambio de favorito se guardó y se procesará cuando vuelvas a estar en línea."))
            }
        }
    }

    private suspend fun flushPendingFavoriteActions() {
        if (!isNetworkAvailable.value) {
            Log.d("FoodDetailVM", "Flush called but still offline. Aborting.")
            return
        }

        val pendingActions = pendingFavoriteActionDataSource.getAllActions()
        if (pendingActions.isEmpty()) {
            Log.d("FoodDetailVM", "No pending favorite actions to flush.")
            return
        }
        Log.d("FoodDetailVM", "Found ${pendingActions.size} pending favorite actions to flush.")

        // Obtener el estado más reciente del usuario DESDE EL REPOSITORIO
        // Esto es crucial porque el estado del usuario podría haber cambiado por otras vías.
        var currentUserState = authRepository.currentUser.firstOrNull()
        if (currentUserState == null) {
            Log.e("FoodDetailVM", "Cannot flush actions, current user is null in AuthRepository. Actions will remain queued.")
            return // No se puede procesar sin un usuario base.
        }

        for (action in pendingActions) {
            // Volver a obtener currentUserState para cada acción podría ser más seguro si las acciones tardan mucho
            // o si updateUserUseCase es muy lento y authRepository.updateCurrentUser es rápido.
            // Pero para un bucle rápido, usar y actualizar una copia local (currentUserState) es aceptable.

            val currentSavedProducts = currentUserState!!.savedProducts.toMutableList()
            val productExistsInList = currentSavedProducts.any { it.id == action.productId }
            var needsRemoteUpdateForThisAction = false
            var updatedProductListForThisAction = currentSavedProducts // Lista que se modificará para esta acción

            if (action.shouldBeFavorite) { // Se quería agregar
                if (!productExistsInList) {
                    try {
                        // Intentar obtener el producto. Es vital para agregarlo.
                        val productForAction = homeDataRepository.allProductsFlow.firstOrNull()?.find { it.id == action.productId }
                            ?: getProductByIdUseCase(action.productId) // Fallback a red si no está en caché de home

                        if (productForAction != null) {
                            updatedProductListForThisAction.add(productForAction)
                            needsRemoteUpdateForThisAction = true
                        } else {
                            Log.w("FoodDetailVM", "Flush: Product ${action.productId} to add as favorite was not found. Skipping this part of action.")
                            // No se puede agregar si el producto no se encuentra. Eliminar la acción de la cola.
                            pendingFavoriteActionDataSource.removeAction(action.id)
                            continue // Pasar a la siguiente acción
                        }
                    } catch (e: Exception) {
                        Log.e("FoodDetailVM", "Flush: Could not fetch product ${action.productId} to add as favorite: ${e.message}. Action remains.")
                        // Si hay error de red al buscar el producto, dejar la acción para reintentar.
                        continue // Pasar a la siguiente acción, o 'break' para reintentar todo el flush más tarde.
                        // 'continue' permite que otras acciones (quizás de eliminación) se procesen.
                    }
                } else {
                    Log.d("FoodDetailVM", "Flush: Pending action to add ${action.productId}, but it's already a favorite. Action obsolete.")
                    pendingFavoriteActionDataSource.removeAction(action.id) // Eliminar acción obsoleta
                    continue
                }
            } else { // Se quería quitar
                if (productExistsInList) {
                    updatedProductListForThisAction.removeAll { it.id == action.productId }
                    needsRemoteUpdateForThisAction = true
                } else {
                    Log.d("FoodDetailVM", "Flush: Pending action to remove ${action.productId}, but it's not a favorite. Action obsolete.")
                    pendingFavoriteActionDataSource.removeAction(action.id) // Eliminar acción obsoleta
                    continue
                }
            }

            if (needsRemoteUpdateForThisAction) {
                val userToUpdateOnServer = currentUserState.copy(savedProducts = updatedProductListForThisAction)
                try {
                    Log.d("FoodDetailVM", "Flushing: Attempting to update user ${action.userId} (actual: ${currentUserState.id}) for product ${action.productId} to favorite: ${action.shouldBeFavorite}")
                    // Usar el ID del currentUserState actual para la actualización, no action.userId,
                    // a menos que haya una razón específica para que action.userId sea diferente y correcto.
                    updateUserUseCase(currentUserState.id, userToUpdateOnServer)

                    // Si tiene éxito, actualizar el estado local en AuthRepository y nuestro currentUserState local
                    authRepository.updateCurrentUser(userToUpdateOnServer)
                    currentUserState = userToUpdateOnServer // Mantener nuestro estado local sincronizado

                    pendingFavoriteActionDataSource.removeAction(action.id)
                    _uiEvent.emit(UiEvent.ShowMessage("Acción pendiente de favorito para '${action.productId}' procesada."))
                    Log.i("FoodDetailVM", "Successfully processed pending action for product ${action.productId}")
                } catch (e: Exception) {
                    Log.e("FoodDetailVM", "Flush: Failed to process pending favorite action for product ${action.productId} on server: ${e.message}", e)
                    // La acción permanece en la cola. Si falla la actualización en el servidor,
                    // detenemos el flush para reintentar todas las acciones (incluida esta) más tarde.
                    // No actualizamos currentUserState local si el backend falló.
                    break
                }
            }
            // Si no se necesitó actualización remota (needsRemoteUpdateForThisAction es false),
            // la acción ya se habrá eliminado si era obsoleta.
        }
    }

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }
}