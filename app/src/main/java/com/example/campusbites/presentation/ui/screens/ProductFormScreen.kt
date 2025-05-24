package com.example.campusbites.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campusbites.presentation.ui.viewmodels.ProductFormViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFormScreen(
    restaurantId: String,
    productId: String?, // Null si es para crear nuevo
    navController: NavController,
    viewModel: ProductFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isEditMode = viewModel.isEditMode

    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ProductFormViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(message = event.message, duration = SnackbarDuration.Short)
                }
                is ProductFormViewModel.UiEvent.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Product" else "Create New Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AnimatedVisibility(visible = !isNetworkAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "You are offline. Changes will be queued and applied when you're back online.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.formError?.contains("Name", ignoreCase = true) == true ||
                        uiState.formError?.contains("All fields", ignoreCase = true) == true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = uiState.formError?.contains("Description", ignoreCase = true) == true ||
                        uiState.formError?.contains("All fields", ignoreCase = true) == true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text("Price (e.g., 15000)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = uiState.formError?.contains("Price", ignoreCase = true) == true ||
                        uiState.formError?.contains("All fields", ignoreCase = true) == true
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.photoUrl,
                onValueChange = viewModel::onPhotoUrlChange,
                label = { Text("Photo URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.formError?.contains("Photo", ignoreCase = true) == true ||
                        uiState.formError?.contains("All fields", ignoreCase = true) == true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Food Tags (select at least one)", style = MaterialTheme.typography.titleMedium)
            if (uiState.formError?.contains("food tag", ignoreCase = true) == true) {
                Text(
                    text = uiState.formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.allFoodTags.forEach { tag ->
                    FilterChip(
                        selected = uiState.selectedFoodTagIds.contains(tag.id),
                        onClick = { viewModel.toggleFoodTag(tag.id) },
                        label = { Text(tag.name) },
                        leadingIcon = if (uiState.selectedFoodTagIds.contains(tag.id)) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Dietary Tags (optional)", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.allDietaryTags.forEach { tag ->
                    FilterChip(
                        selected = uiState.selectedDietaryTagIds.contains(tag.id),
                        onClick = { viewModel.toggleDietaryTag(tag.id) },
                        label = { Text(tag.name) },
                        leadingIcon = if (uiState.selectedDietaryTagIds.contains(tag.id)) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Ingredientes
            Text("Ingredients (optional)", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Asegura espaciado vertical también
            ) {
                uiState.allIngredients.forEach { ingredient ->
                    FilterChip(
                        selected = uiState.selectedIngredientIds.contains(ingredient.id),
                        onClick = { viewModel.toggleIngredient(ingredient.id) },
                        label = { Text(ingredient.name) },
                        leadingIcon = if (uiState.selectedIngredientIds.contains(ingredient.id)) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))


            if (uiState.formError != null && !(uiState.formError!!.contains("food tag", ignoreCase = true))) {
                Text(
                    text = uiState.formError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.saveProduct() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isEditMode) "Update Product" else "Create Product")
                }
            }
        }
    }
}