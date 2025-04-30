package com.example.campusbites.presentation.ui.screens

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.campusbites.presentation.ui.viewmodels.FoodDetailViewModel
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    foodId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val product by viewModel.product.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val currentUser by authViewModel.user.collectAsState()

    // Cargar detalles
    LaunchedEffect(foodId) { viewModel.loadFoodDetail(foodId) }

    val isFavorite by remember(currentUser, product) {
        derivedStateOf { currentUser?.savedProducts?.any { it.id == product?.id } == true }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = product?.name ?: "Cargando...", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { handleSaveClick(coroutineScope, authViewModel, viewModel, product?.id) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Eliminar favorito" else "Agregar favorito"
                        )
                    }
//                    IconButton(onClick = { /* TODO: lógica de compartir */ }) {
//                        Icon(Icons.Filled.Share, contentDescription = "Compartir")
//                    }
                }
            )
        }
    ) { paddingValues ->
        product?.let {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// Función auxiliar fuera del Composable
private fun handleSaveClick(
    coroutineScope: CoroutineScope,
    authViewModel: AuthViewModel,
    viewModel: FoodDetailViewModel,
    productId: String?
) {
    if (productId == null) return
    coroutineScope.launch {
        val currentUser = authViewModel.user.value ?: return@launch
        val saved = currentUser.savedProducts.orEmpty().toMutableList()
        if (saved.any { it.id == productId }) saved.removeAll { it.id == productId }
        else viewModel.product.value?.let { saved.add(it) }
        val updatedUser = currentUser.copy(savedProducts = saved)
        authViewModel.updateUser(updatedUser)
        viewModel.onSaveClick(updatedUser)
    }
}
