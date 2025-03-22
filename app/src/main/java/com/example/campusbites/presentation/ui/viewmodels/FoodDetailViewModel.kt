package com.example.campusbites.presentation.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FoodDetailViewModel @Inject constructor(
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase
) : ViewModel() {

    private val _product = MutableStateFlow<ProductDomain?>(null)
    val product: StateFlow<ProductDomain?> = _product

    private val _ingredients = MutableStateFlow<List<IngredientDomain>>(emptyList())
    val ingredients: StateFlow<List<IngredientDomain>> = _ingredients

    fun loadFoodDetail(foodId: String) {
        viewModelScope.launch {
            val loadedProduct = getProductByIdUseCase(foodId)
            _product.value = loadedProduct

            loadedProduct.let { product ->
                val allIngredients = getIngredientsUseCase()
                _ingredients.value = allIngredients.filter { ingredient ->
                    product.ingredientsIds.contains(ingredient.id)
                }
            }
        }
    }
}