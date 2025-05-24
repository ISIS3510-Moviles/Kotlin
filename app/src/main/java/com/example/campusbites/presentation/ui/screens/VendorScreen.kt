package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.presentation.navigation.NavigationRoutes
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.VendorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    vendorViewModel: VendorViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.user.collectAsState()
    val vendorRestaurantId = currentUser?.vendorRestaurantId

    val restaurant by vendorViewModel.restaurant.collectAsState()
    val isLoading by vendorViewModel.isLoading.collectAsState()
    val errorMessage by vendorViewModel.errorMessage.collectAsState()
    val isNetworkAvailable by vendorViewModel.isNetworkAvailable.collectAsState()


    LaunchedEffect(vendorRestaurantId) {
        if (!vendorRestaurantId.isNullOrBlank()) {
            vendorViewModel.loadVendorRestaurant(vendorRestaurantId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Manage Restaurant") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp))
                } else if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    // Opción de reintentar si el error no es por falta de ID de vendor
                    if (errorMessage != "Vendor restaurant ID is missing." &&
                        errorMessage != "You are not currently assigned as a vendor to any restaurant.") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            if (!vendorRestaurantId.isNullOrBlank()) {
                                vendorViewModel.loadVendorRestaurant(vendorRestaurantId)
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                } else if (restaurant == null && !vendorRestaurantId.isNullOrBlank() && !isNetworkAvailable) {
                    // Este caso es cuando no hay caché y no hay red
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            "No internet connection and restaurant details not found in cache. Please connect to the internet and try again.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else if (restaurant != null) {
                    RestaurantDetailsSection(restaurant = restaurant!!)

                    Spacer(modifier = Modifier.height(24.dp))

                    VendorActionButton(
                        text = "Manage Products",
                        icon = Icons.Filled.Edit,
                        onClick = {
                            navController.navigate(NavigationRoutes.createManageProductsRoute(restaurant!!.id))
                        }
                    )
                    VendorActionButton(
                        text = "View Reservations",
                        icon = Icons.Filled.List,
                        onClick = {
                            navController.navigate(NavigationRoutes.VENDOR_RESERVATIONS)
                        }
                    )
                    } else if (vendorRestaurantId.isNullOrBlank()){
                    Text(
                        "You are not currently assigned as a vendor to any restaurant.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Loading restaurant information...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
private fun VendorActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        enabled = enabled,
        contentPadding = PaddingValues(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

@Composable
private fun RestaurantDetailsSection(restaurant: RestaurantDomain) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = restaurant.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = restaurant.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Contacto
        Text(
            text = "Contact Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Address:", value = restaurant.address)
        DetailRow(label = "Phone:", value = restaurant.phone)
        DetailRow(label = "Email:", value = restaurant.email)
        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Horarios
        Text(
            text = "Operating Hours",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Opening Time:", value = restaurant.openingTime)
        DetailRow(label = "Closing Time:", value = restaurant.closingTime)
        DetailRow(label = "Opens Weekends:", value = if (restaurant.opensWeekends) "Yes" else "No")
        DetailRow(label = "Opens Holidays:", value = if (restaurant.opensHolidays) "Yes" else "No")
        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Estado y Rating
        Text(
            text = "Status & Rating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "Active:", value = if (restaurant.isActive) "Yes" else "No")
        DetailRow(label = "Rating:", value = String.format("%.1f", restaurant.rating))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f) // Ocupa el 40% del ancho
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f) // Ocupa el 60% del ancho
        )
    }
    Spacer(modifier = Modifier.height(4.dp)) // Espacio entre filas de detalles
}