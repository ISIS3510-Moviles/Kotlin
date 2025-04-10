package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCreateScreen(
    onBackClick: () -> Unit,
    onCreateClick: (String, String, String) -> Unit,
    restaurants: List<RestaurantDomain> = getSampleRestaurants(),
    authViewModel: AuthViewModel
) {
    var description by remember { mutableStateOf("") }
    var selectedRestaurantId by remember { mutableStateOf("") }
    var selectedRestaurantName by remember { mutableStateOf("") }
    var isRestaurantMenuExpanded by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            AlertTopBar(onBackClick = onBackClick)

            Text(
                text = "Create New Alert",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                minLines = 3
            )

            // Dropdown para seleccionar restaurante
            ExposedDropdownMenuBox(
                expanded = isRestaurantMenuExpanded,
                onExpandedChange = { isRestaurantMenuExpanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = selectedRestaurantName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Restaurant") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRestaurantMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = isRestaurantMenuExpanded,
                    onDismissRequest = { isRestaurantMenuExpanded = false }
                ) {
                    restaurants.forEach { restaurant ->
                        DropdownMenuItem(
                            text = { Text(restaurant.name) },
                            onClick = {
                                selectedRestaurantName = restaurant.name
                                selectedRestaurantId = restaurant.id
                                isRestaurantMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { authViewModel.user.value?.let { onCreateClick(description, selectedRestaurantId, it.id) } },
                enabled = description.isNotBlank() && selectedRestaurantId.isNotBlank(),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 16.dp)
            ) {
                Text("Create Alert")
            }
        }
    }
}

// Función para obtener datos de prueba de restaurantes
private fun getSampleRestaurants(): List<RestaurantDomain> {
    return listOf(
        RestaurantDomain(
            id = "1",
            name = "Cafetería Central",
            description = "Cafetería principal del campus",
            latitude = 4.6371,
            longitude = -74.0846,
            routeIndications = "Edificio central, primer piso",
            openingTime = "07:00",
            closingTime = "19:00",
            opensHolidays = false,
            opensWeekends = true,
            isActive = true,
            rating = 4.2,
            address = "Calle Principal #123",
            phone = "123456789",
            email = "cafe@universidad.edu",
            overviewPhoto = "https://ejemplo.com/foto1.jpg",
            profilePhoto = "https://ejemplo.com/perfil1.jpg",
            photos = listOf("https://ejemplo.com/foto1.jpg"),
            foodTags = emptyList(),
            dietaryTags = emptyList(),
        ),
        RestaurantDomain(
            id = "2",
            name = "Pizza Campus",
            description = "Las mejores pizzas del campus",
            latitude = 4.6381,
            longitude = -74.0856,
            routeIndications = "Edificio de ingeniería, planta baja",
            openingTime = "11:00",
            closingTime = "22:00",
            opensHolidays = true,
            opensWeekends = true,
            isActive = true,
            rating = 4.7,
            address = "Avenida Universidad #456",
            phone = "987654321",
            email = "pizza@universidad.edu",
            overviewPhoto = "https://ejemplo.com/foto2.jpg",
            profilePhoto = "https://ejemplo.com/perfil2.jpg",
            photos = listOf("https://ejemplo.com/foto2.jpg"),
            foodTags = emptyList(),
            dietaryTags = emptyList(),
        ),
        RestaurantDomain(
            id = "3",
            name = "Sushi Universitario",
            description = "Sushi fresco para estudiantes",
            latitude = 4.6391,
            longitude = -74.0866,
            routeIndications = "Edificio de ciencias, segundo piso",
            openingTime = "12:00",
            closingTime = "20:00",
            opensHolidays = false,
            opensWeekends = false,
            isActive = true,
            rating = 4.5,
            address = "Carrera 15 #789",
            phone = "456789123",
            email = "sushi@universidad.edu",
            overviewPhoto = "https://ejemplo.com/foto3.jpg",
            profilePhoto = "https://ejemplo.com/perfil3.jpg",
            photos = listOf("https://ejemplo.com/foto3.jpg"),
            foodTags = emptyList(),
            dietaryTags = emptyList(),
        )
    )
}
