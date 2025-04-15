package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.campusbites.domain.usecase.user.UpdateUserUseCase
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.FoodDetailViewModel
import kotlinx.coroutines.launch

@Composable
fun FoodDetailScreen(
    foodId: String,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel(),
    authViewModel: AuthViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    val onSaveClick: (String) -> Unit = { productId ->
        coroutineScope.launch {
            val currentUser = authViewModel.user.value

            if (currentUser == null) {
                Log.e("FoodDetailScreen", "❌ Usuario no disponible")
                return@launch
            }

            val alreadySaved = currentUser.savedProducts.any { it.id == productId }

            if (alreadySaved) {
                Log.d("FoodDetailScreen", "✅ El producto ya está guardado")
            } else {
                val updatedProducts = currentUser.savedProducts.toMutableList()
                viewModel.product.value?.let { updatedProducts.add(it) }

                val updatedUser = currentUser.copy(savedProducts = updatedProducts)
                authViewModel.setUser(updatedUser)
                viewModel.onSaveClick(updatedUser)
            }
        }
    }



    LaunchedEffect(key1 = foodId) {
        viewModel.loadFoodDetail(foodId)
    }

    val product by viewModel.product.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    if (product == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            AsyncImage(
                model = product!!.photo,
                contentDescription = product!!.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = product!!.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$${product!!.price}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "★ ${product!!.rating}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = product!!.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            ingredients.forEach { ingredient ->
                Text(
                    text = "• ${ingredient.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Divider()
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onSaveClick(product!!.id) },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Favorites")
            }
        }
    }
}
