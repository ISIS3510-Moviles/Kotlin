package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.usecase.product.GetProductsByRestaurantUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantByIdUseCase
import com.example.campusbites.domain.usecase.comment.GetCommentsUseCase
import com.example.campusbites.domain.usecase.restaurant.GetRestaurantsUseCase
import com.google.firebase.perf.ktx.performance
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestaurantDetailViewModel @Inject constructor(
    private val getRestaurantByIdUseCase: GetRestaurantByIdUseCase,
    private val getProductsByRestaurantUseCase: GetProductsByRestaurantUseCase,
    private val getReviewsByRestaurantUseCase: GetCommentsUseCase,
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantDetailUiState())
    val uiState: StateFlow<RestaurantDetailUiState> = _uiState

    private val _restaurants = MutableStateFlow<List<RestaurantDomain>>(emptyList())
    val restaurants: StateFlow<List<RestaurantDomain>> = _restaurants

    fun loadRestaurantDetails(restaurantId: String) {
        viewModelScope.launch {

            val restaurant = getRestaurantByIdUseCase(restaurantId)

            val products = getProductsByRestaurantUseCase(restaurantId)

            val reviews = getReviewsByRestaurantUseCase(restaurantId)

            _uiState.update { currentState ->
                currentState.copy(
                    restaurant = restaurant,
                    products = products,
                    reviews = reviews,
                    popularProducts = products.sortedByDescending { it.rating }.take(5),
                    under20Products = products.filter { it.price <= 20000 }
                )
            }
        }
    }

    fun loadAllRestaurants() {
        viewModelScope.launch {
            val allRestaurants = getRestaurantsUseCase()
            _restaurants.value = allRestaurants
        }
    }
}

data class RestaurantDetailUiState(
    val restaurant: RestaurantDomain? = null,
    val products: List<ProductDomain> = emptyList(),
    val reviews: List<CommentDomain> = emptyList(),
    val popularProducts: List<ProductDomain> = emptyList(),
    val under20Products: List<ProductDomain> = emptyList()
)