package com.example.campusbites.presentation.ui.viewmodels

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.model.IngredientDomain
import com.example.campusbites.domain.usecase.product.CreateProductUseCase
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.product.UpdateProductUseCase
import com.example.campusbites.domain.usecase.tag.GetDietaryTagsUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagsUseCase
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getFoodTagsUseCase: GetFoodTagsUseCase,
    private val getDietaryTagsUseCase: GetDietaryTagsUseCase,
    private val getIngredientsUseCase: GetIngredientsUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val firebaseAnalytics: FirebaseAnalytics
) : ViewModel() {

    val restaurantId: String = savedStateHandle.get<String>("restaurantId") ?: ""
    private val productId: String? = savedStateHandle.get<String>("productId")

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    val isNetworkAvailable: StateFlow<Boolean> = connectivityMonitor.isNetworkAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isEditMode: Boolean = productId != null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, formError = null) }
            try {
                val foodTags = getFoodTagsUseCase()
                val dietaryTags = getDietaryTagsUseCase()
                val ingredients = getIngredientsUseCase()

                _uiState.update {
                    it.copy(
                        allFoodTags = foodTags,
                        allDietaryTags = dietaryTags,
                        allIngredients = ingredients
                    )
                }

                if (isEditMode && productId != null) {
                    val product = getProductByIdUseCase(productId)
                    if (product != null) {
                        _uiState.update {
                            it.copy(
                                name = product.name,
                                description = product.description,
                                price = product.price.toString(),
                                photoUrl = product.photo,
                                selectedFoodTagIds = product.foodTags.map { tag -> tag.id }.toSet(),
                                selectedDietaryTagIds = product.dietaryTags.map { tag -> tag.id }.toSet(),
                                selectedIngredientIds = product.ingredientsIds.toSet(),
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, formError = "Product not found. It might have been deleted or there was an issue loading it.") }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("ProductFormVM", "Error loading initial data: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, formError = "Failed to load initial data: ${e.localizedMessage}") }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name, formError = null) }
    fun onDescriptionChange(description: String) = _uiState.update { it.copy(description = description, formError = null) }
    fun onPriceChange(price: String) = _uiState.update { it.copy(price = price, formError = null) }
    fun onPhotoUrlChange(url: String) = _uiState.update { it.copy(photoUrl = url, formError = null) }

    fun toggleFoodTag(tagId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedFoodTagIds.toMutableSet()
            if (currentSelected.contains(tagId)) currentSelected.remove(tagId)
            else currentSelected.add(tagId)
            currentState.copy(selectedFoodTagIds = currentSelected, formError = null)
        }
    }

    fun toggleDietaryTag(tagId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedDietaryTagIds.toMutableSet()
            if (currentSelected.contains(tagId)) currentSelected.remove(tagId)
            else currentSelected.add(tagId)
            currentState.copy(selectedDietaryTagIds = currentSelected, formError = null)
        }
    }

    fun toggleIngredient(ingredientId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedIngredientIds.toMutableSet()
            if (currentSelected.contains(ingredientId)) currentSelected.remove(ingredientId)
            else currentSelected.add(ingredientId)
            currentState.copy(selectedIngredientIds = currentSelected, formError = null)
        }
    }

    fun saveProduct() {
        val currentState = _uiState.value
        val priceFloat = currentState.price.toFloatOrNull()
        val photoToSave = currentState.photoUrl.ifBlank { currentState.defaultPhotoUrl }


        if (currentState.name.isBlank() || currentState.description.isBlank() || priceFloat == null || priceFloat <= 0 ) {
            _uiState.update { it.copy(formError = "Name, description, and a valid price are required.") }
            return
        }
        if (currentState.selectedFoodTagIds.isEmpty()){
            _uiState.update { it.copy(formError = "Please select at least one food tag.") }
            return
        }

        _uiState.update { it.copy(formError = null, isLoading = true) }

        viewModelScope.launch {
            try {
                if (isEditMode && productId != null) {
                    val updateDto = UpdateProductDTO(
                        name = currentState.name,
                        description = currentState.description,
                        price = priceFloat,
                        photo = photoToSave,
                        foodTagsIds = currentState.selectedFoodTagIds.toList(),
                        dietaryTagsIds = currentState.selectedDietaryTagIds.toList(),
                        ingredientsIds = currentState.selectedIngredientIds.toList()
                    )
                    updateProductUseCase(productId, updateDto)
                    _uiEvent.emit(UiEvent.ShowMessage("Product updated successfully!"))
                } else {
                    val createDto = CreateProductDTO(
                        name = currentState.name,
                        description = currentState.description,
                        price = priceFloat,
                        photo = photoToSave,
                        restaurant_id = restaurantId,
                        foodTagsIds = currentState.selectedFoodTagIds.toList(),
                        dietaryTagsIds = currentState.selectedDietaryTagIds.toList(),
                        ingredientsIds = currentState.selectedIngredientIds.toList()
                    )
                    val createdProduct = createProductUseCase(createDto)
                    _uiEvent.emit(UiEvent.ShowMessage("Product created successfully!"))

                    val params = Bundle().apply {
                        putString("restaurant_id", restaurantId)
                        putString("product_id", createdProduct.id)
                        putString("product_name", createdProduct.name)
                    }
                    firebaseAnalytics.logEvent("product_added", params)
                    Log.d("Analytics", "Logged product_added event for restaurant: $restaurantId, product: ${createdProduct.name}")
                }
                _uiEvent.emit(UiEvent.NavigateBack)
            } catch (e: Exception) {
                Log.e("ProductFormVM", "Error saving product: ${e.message}", e)
                val message = if (e.message?.contains("Offline", ignoreCase = true) == true ||
                    e.message?.contains("queued", ignoreCase = true) == true) {
                    "Offline: Product changes queued."
                } else {
                    "Error saving product: ${e.localizedMessage ?: "Unknown error"}"
                }
                _uiEvent.emit(UiEvent.ShowMessage(message))

                if (message.startsWith("Offline")) { // Navigate back if queued
                    _uiEvent.emit(UiEvent.NavigateBack)
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    data class ProductFormUiState(
        val name: String = "",
        val description: String = "",
        val price: String = "",
        val photoUrl: String = "", // Puede ser vacío si el usuario no ingresa nada
        val defaultPhotoUrl: String = "https://firebasestorage.googleapis.com/v0/b/campusbites-d6b0b.appspot.com/o/placeholder%2Fproduct_placeholder.png?alt=media&token=52518919-9e50-4308-8d93-08e837f911f0",
        val selectedFoodTagIds: Set<String> = emptySet(),
        val selectedDietaryTagIds: Set<String> = emptySet(),
        val selectedIngredientIds: Set<String> = emptySet(),
        val allFoodTags: List<FoodTagDomain> = emptyList(),
        val allDietaryTags: List<DietaryTagDomain> = emptyList(),
        val allIngredients: List<IngredientDomain> = emptyList(),
        val isLoading: Boolean = false,
        val formError: String? = null
    )

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}