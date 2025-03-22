package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.FoodDetailViewModel

@Composable
fun FoodDetailScreen(
    foodId: String,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val onSaveClick: (String) -> Unit = { productId ->
        authViewModel.addProductToUser(
            productId = productId,
            onSuccess = {
                Log.d("UI", "Producto añadido con éxito")
            },
            onFailure = { exception ->
                Log.e("UI", "Error al añadir producto: ${exception.message}")
            }
        )
    }

    // Carga los datos solo una vez
    LaunchedEffect(key1 = foodId) {
        viewModel.loadFoodDetail(foodId)
    }

    val product by viewModel.product.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    if (product == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Mostrar la foto del producto
            AsyncImage(
                model = product!!.photo,
                contentDescription = product!!.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre del producto
            Text(
                text = product!!.name,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Precio del producto
            Text(
                text = "$${product!!.price}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating del producto
            Text(
                text = "Rating: ${product!!.rating}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción del producto
            Text(
                text = product!!.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Listado de ingredientes filtrados
            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleMedium
            )
            ingredients.forEach { ingredient ->
                Text(text = "• ${ingredient.name}")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón para guardar el producto como favorito
            Button(
                onClick = { onSaveClick(product!!.id) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Favorites"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Favorites")
            }
        }
    }
}
