package com.example.campusbites.presentation.ui.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.data.dto.CreateProductDTO
import com.example.campusbites.data.dto.UpdateProductDTO
import com.example.campusbites.data.network.ConnectivityMonitor
import com.example.campusbites.domain.model.DietaryTagDomain
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.model.IngredientDomain // Importar IngredientDomain
import com.example.campusbites.domain.usecase.product.CreateProductUseCase
import com.example.campusbites.domain.usecase.product.GetIngredientsUseCase // Importar GetIngredientsUseCase
import com.example.campusbites.domain.usecase.product.GetProductByIdUseCase
import com.example.campusbites.domain.usecase.product.UpdateProductUseCase
import com.example.campusbites.domain.usecase.tag.GetDietaryTagsUseCase
import com.example.campusbites.domain.usecase.tag.GetFoodTagsUseCase
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
    private val getIngredientsUseCase: GetIngredientsUseCase, // Inyectar GetIngredientsUseCase
    private val connectivityMonitor: ConnectivityMonitor
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
            _uiState.update { it.copy(isLoading = true) }
            try {
                val foodTags = getFoodTagsUseCase()
                val dietaryTags = getDietaryTagsUseCase()
                val ingredients = getIngredientsUseCase() // Cargar todos los ingredientes
                _uiState.update { it.copy(
                    allFoodTags = foodTags,
                    allDietaryTags = dietaryTags,
                    allIngredients = ingredients // Actualizar estado con todos los ingredientes
                )}

                if (isEditMode && productId != null) {
                    val product = getProductByIdUseCase(productId)
                    _uiState.update {
                        it.copy(
                            name = product.name,
                            description = product.description,
                            price = product.price.toString(),
                            photoUrl = product.photo,
                            selectedFoodTagIds = product.foodTags.map { tag -> tag.id }.toSet(),
                            selectedDietaryTagIds = product.dietaryTags.map { tag -> tag.id }.toSet(),
                            selectedIngredientIds = product.ingredientsIds.toSet(), // Pre-seleccionar ingredientes
                            isLoading = false
                        )
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

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onDescriptionChange(description: String) = _uiState.update { it.copy(description = description) }
    fun onPriceChange(price: String) = _uiState.update { it.copy(price = price) }
    fun onPhotoUrlChange(url: String) = _uiState.update { it.copy(photoUrl = url) }

    fun toggleFoodTag(tagId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedFoodTagIds.toMutableSet()
            if (currentSelected.contains(tagId)) currentSelected.remove(tagId)
            else currentSelected.add(tagId)
            currentState.copy(selectedFoodTagIds = currentSelected)
        }
    }

    fun toggleDietaryTag(tagId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedDietaryTagIds.toMutableSet()
            if (currentSelected.contains(tagId)) currentSelected.remove(tagId)
            else currentSelected.add(tagId)
            currentState.copy(selectedDietaryTagIds = currentSelected)
        }
    }

    fun toggleIngredient(ingredientId: String) { // Nueva función para ingredientes
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedIngredientIds.toMutableSet()
            if (currentSelected.contains(ingredientId)) currentSelected.remove(ingredientId)
            else currentSelected.add(ingredientId)
            currentState.copy(selectedIngredientIds = currentSelected)
        }
    }

    fun saveProduct() {
        val currentState = _uiState.value
        val priceFloat = currentState.price.toFloatOrNull()

        if (currentState.name.isBlank() || currentState.description.isBlank() || priceFloat == null || priceFloat <= 0 || currentState.photoUrl.isBlank()) {
            _uiState.update { it.copy(formError = "Name, description, valid price, and photo URL are required.") }
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
                        photo = currentState.photoUrl,
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
                        photo = currentState.photoUrl,
                        restaurant_id = restaurantId,
                        foodTagsIds = currentState.selectedFoodTagIds.toList(),
                        dietaryTagsIds = currentState.selectedDietaryTagIds.toList(),
                        ingredientsIds = currentState.selectedIngredientIds.toList()
                    )
                    createProductUseCase(createDto)
                    _uiEvent.emit(UiEvent.ShowMessage("Product created successfully!"))
                }
                _uiEvent.emit(UiEvent.NavigateBack)
            } catch (e: Exception) {
                Log.e("ProductFormVM", "Error saving product: ${e.message}", e)
                val message = if (e.message?.contains("Offline") == true || e.message?.contains("queued") == true) {
                    "Offline: Product changes queued."
                } else {
                    "Error saving product: ${e.localizedMessage}"
                }
                _uiEvent.emit(UiEvent.ShowMessage(message))

                if (e.message?.contains("Offline") == true || e.message?.contains("queued") == true) {
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
        val photoUrl: String = "",
        val selectedFoodTagIds: Set<String> = emptySet(),
        val selectedDietaryTagIds: Set<String> = emptySet(),
        val selectedIngredientIds: Set<String> = emptySet(), // Nuevo estado para ingredientes seleccionados
        val allFoodTags: List<FoodTagDomain> = emptyList(),
        val allDietaryTags: List<DietaryTagDomain> = emptyList(),
        val allIngredients: List<IngredientDomain> = emptyList(), // Nuevo estado para todos los ingredientes
        val isLoading: Boolean = false,
        val formError: String? = null
    )

    sealed class UiEvent {
        data class ShowMessage(val message: String) : UiEvent()
        object NavigateBack : UiEvent()
    }
}