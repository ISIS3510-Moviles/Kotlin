package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue // Importar para el delegado 'by'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType // Importar KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.campusbites.data.dto.UpdateRestaurantDTO
import com.example.campusbites.presentation.ui.viewmodels.EditRestaurantViewModel
import com.example.campusbites.presentation.ui.viewmodels.EditRestaurantUiState // Asegúrate de que este import sea correcto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRestaurantScreen(
    navController: NavHostController,
    restaurantId: String,
    viewModel: EditRestaurantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState() // Usar 'by' delegado para acceder directamente a las propiedades

    LaunchedEffect(restaurantId) {
        viewModel.loadRestaurantForEdit(restaurantId)
    }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            navController.popBackStack() // Navegar de vuelta al éxito
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Restaurant") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                } else if (uiState.errorMessage != null) {
                    Text(
                        text = "Error: ${uiState.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { viewModel.loadRestaurantForEdit(restaurantId) }) {
                        Text("Retry Load")
                    }
                } else if (uiState.restaurant == null) {
                    Text("Restaurant not found or could not be loaded.", modifier = Modifier.padding(16.dp))
                } else {
                    Text(
                        text = "Restaurant Information",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.mainPhotoLink,
                        onValueChange = viewModel::onMainPhotoLinkChange, // Usar referencia a función
                        label = { Text("Main Photo Link") },
                        placeholder = { Text("Enter URL for main photo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.homePhotoLink,
                        onValueChange = viewModel::onHomePhotoLinkChange,
                        label = { Text("Home Photo Link") },
                        placeholder = { Text("Enter URL for home photo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Restaurant Description") },
                        placeholder = { Text("The description of the restaurant") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Coordinates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.latitude,
                            onValueChange = viewModel::onLatitudeChange,
                            label = { Text("Latitude") },
                            placeholder = { Text("latitude") },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.longitude,
                            onValueChange = viewModel::onLongitudeChange,
                            label = { Text("Longitude") },
                            placeholder = { Text("longitude") },
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uiState.locationDescription,
                        onValueChange = viewModel::onLocationDescriptionChange,
                        label = { Text("Location Description") },
                        placeholder = { Text("The description of the location") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val updateDto = UpdateRestaurantDTO(
                                name = uiState.restaurant?.name,
                                description = uiState.description.ifBlank { null },
                                latitude = uiState.latitude.toDoubleOrNull(),
                                longitude = uiState.longitude.toDoubleOrNull(),
                                address = uiState.locationDescription.ifBlank { null },
                                overviewPhoto = uiState.mainPhotoLink.ifBlank { null },
                                profilePhoto = uiState.homePhotoLink.ifBlank { null }
                            )
                            viewModel.updateRestaurant(restaurantId, updateDto)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isUpdating
                    ) {
                        if (uiState.isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Update")
                        }
                    }
                }
            }
        }
    )
}