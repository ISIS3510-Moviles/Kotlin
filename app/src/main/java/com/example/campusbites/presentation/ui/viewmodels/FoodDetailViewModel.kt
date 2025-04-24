package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.preferences.HomeDataRepository
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val homeDataRepository: HomeDataRepository,
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    private val _product = MutableStateFlow<ProductDomain?>(null)
    val product: StateFlow<ProductDomain?> = _product

    private val _ingredients = MutableStateFlow<List<IngredientDomain>>(emptyList())
    val ingredients: StateFlow<List<IngredientDomain>> = _ingredients

    private val isOnline = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                trySend(true)
            }

            override fun onLost(network: android.net.Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        trySend(isConnected)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }.distinctUntilChanged()

    fun loadFoodDetail(foodId: String) {
        viewModelScope.launch {
            val cachedProduct = homeDataRepository.allProductsFlow.firstOrNull()?.find { it.id == foodId }
            if (cachedProduct != null) {
                _product.value = cachedProduct
                val allIngredients = homeDataRepository.allIngredientsFlow.firstOrNull() ?: emptyList()
                _ingredients.value = allIngredients.filter { ingredient ->
                    cachedProduct.ingredientsIds.contains(ingredient.id)
                }
            }

            if (isOnline.first()) {
                fetchFoodDetail(foodId)
            }
        }
    }

    private suspend fun fetchFoodDetail(foodId: String) {
        try {
            val loadedProduct = getProductByIdUseCase(foodId)
            _product.value = loadedProduct

            loadedProduct.let { product ->
                val allIngredients = getIngredientsUseCase()
                _ingredients.value = allIngredients.filter { ingredient ->
                    product.ingredientsIds.contains(ingredient.id)
                }
            }
        } catch (e: Exception) {
        }
    }

    suspend fun onSaveClick(user: UserDomain) {
        updateUserUseCase(user.id, user)
    }

}