package com.example.campusbites.presentation.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.campusbites.presentation.ui.viewmodels.FoodDetailViewModel
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition") // Revísalo, idealmente recolectarías con collectAsState
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
) {
    val product by viewModel.product.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val currentUser by authViewModel.user.collectAsState() // Este es el UserDomain de AuthViewModel

    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState() // RECOGER ESTADO DE RED
    val snackbarHostState = remember { SnackbarHostState() } // PARA SNACKBARS

    // Cargar detalles
    LaunchedEffect(foodId) {
        viewModel.loadFoodDetail(foodId)
    }

    // Escuchar eventos de UI del ViewModel
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is FoodDetailViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is FoodDetailViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long,
                        // Puedes añadir un action para reintentar, etc.
                    )
                }
            }
        }
    }

    val isFavorite by remember(currentUser, product) {
        derivedStateOf {
            currentUser?.savedProducts?.any { it.id == product?.id } == true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // AÑADIR SNACKBARHOST
        topBar = {
            TopAppBar(
                title = { Text(text = product?.name ?: "Cargando...", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val currentProduct = product
                            val user = currentUser
                            if (user != null && currentProduct != null) {
                                // 1. Actualizar UI localmente a través de AuthViewModel (Optimistic Update)
                                val updatedSavedProducts = user.savedProducts.toMutableList()
                                val currentlyIsFav = updatedSavedProducts.any { it.id == currentProduct.id }

                                if (!currentlyIsFav) { // Si no es favorito, lo vamos a agregar
                                    updatedSavedProducts.add(currentProduct)
                                } else { // Si es favorito, lo vamos a quitar
                                    updatedSavedProducts.removeAll { it.id == currentProduct.id }
                                }
                                val optimisticallyUpdatedUser = user.copy(savedProducts = updatedSavedProducts)
                                authViewModel.updateUser(optimisticallyUpdatedUser) // Esto actualiza el currentUser y el DataStore

                                // 2. Dejar que el FoodDetailViewModel maneje la lógica de red/cola
                                viewModel.handleFavoriteToggleLogic(
                                    currentUser = user,
                                    productId = currentProduct?.id, // Añadir safe call por si currentProduct es null
                                    productObjectForUi = currentProduct, // <--- Nombre corregido
                                    isCurrentlyFavorite = currentlyIsFav
                                )
                            } else {
                                Log.w("FoodDetailScreen", "User or Product is null, cannot toggle favorite.")
                                // Opcionalmente mostrar un snackbar de error aquí también
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Eliminar favorito" else "Agregar favorito",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) { // Box para superponer el banner
            product?.let {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        // Añadir padding inferior si el banner está abajo y puede solaparse
                        .padding(bottom = if (!isNetworkAvailable) 56.dp else 0.dp)
                ) {
                    // ... resto del contenido de LazyColumn (items) ...
                    item {
                        Card(
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .size(280.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            AsyncImage(
                                model = product!!.photo,
                                contentDescription = product!!.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                            )

                        }
                    }

                    item {
                        AssistChip(
                            enabled = false,
                            onClick = { /* TODO: acción rating */ },
                            label = { Text(text = "★ ${product!!.rating}") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = product!!.name,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "\$${product!!.price}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                        }
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("What's this?", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(product!!.description, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    item {
                        Row (
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ingredients",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(start = 20.dp, top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(ingredients) { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp)) // Espacio para que no se solape con el banner de offline
                    }
                }
            } ?: run {
                // Placeholder o indicador de carga si el producto es null
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // BANNER DE OFFLINE
            AnimatedVisibility(
                visible = !isNetworkAvailable, // Esto debería funcionar si isNetworkAvailable es false
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        // .background(MaterialTheme.colorScheme.errorContainer) // Surface ya toma el color
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer, // Color de fondo del Surface
                    tonalElevation = 4.dp // Opcional: para darle un poco de elevación
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // Padding interno
                    ) {
                        Icon(
                            // Icons.Filled.Info, // Cambiado
                            imageVector = Icons.Filled.Info, // O el icono que prefieras para offline
                            contentDescription = "Offline",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Estás desconectado. Algunas funciones pueden no estar disponibles.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}