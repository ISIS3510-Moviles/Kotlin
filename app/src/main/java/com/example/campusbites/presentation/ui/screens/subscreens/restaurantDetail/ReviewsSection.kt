package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusbites.domain.model.CommentDomain
import com.example.campusbites.presentation.ui.components.CommentCard
import com.example.campusbites.presentation.ui.components.ReviewDialog
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReviewsSection(
    restaurantDetailViewModel: RestaurantDetailViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val uiState by restaurantDetailViewModel.uiState.collectAsState() // Usar 'by' para desestructurar
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Firebase.analytics.logEvent("restaurant_reviews_checked", null)
    }

    // **MODIFICACIÓN CLAVE AQUÍ:** Ordenar las reviews
    val sortedReviews = remember(uiState.reviews) { // remember para evitar re-ordenar en cada recomposición innecesaria
        uiState.reviews.sortedByDescending { comment ->
            try {
                // Intenta parsear la fecha. Si falla, se considera una fecha muy antigua
                // para que los comentarios con fechas inválidas vayan al final.
                OffsetDateTime.parse(comment.datetime)
            } catch (e: Exception) {
                // Loguea el error si el formato de fecha es incorrecto
                // Puedes usar un OffsetDateTime.MIN o similar si quieres que se ordenen por la fecha actual
                // si no se puede parsear la original. Para que vayan al final (más antiguas), puedes usar OffsetDateTime.MIN
                OffsetDateTime.MIN // Esto asegura que los comentarios con fechas no parseables vayan al final
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Reviews", style = MaterialTheme.typography.displayMedium)

        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Write a review")
        }

        // Spacer para dar un poco de espacio entre el botón y las reviews
        Spacer(modifier = Modifier.height(16.dp))

        if (sortedReviews.isEmpty()) {
            Text(
                text = "No reviews yet. Be the first to write one!",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f) // Asegura que el LazyColumn ocupe el espacio restante
                    .padding(horizontal = 16.dp), // Añade padding horizontal para las tarjetas
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sortedReviews) { comment -> CommentCard(comment) } // Usar la lista ordenada
            }
        }
    }

    if (showDialog) {
        ReviewDialog(
            onDismiss = { showDialog = false },
            onSend = { rating, message ->
                showDialog = false
                val user = authViewModel.user.value ?: run {
                    Toast.makeText(context, "You must be logged in to write a review", Toast.LENGTH_SHORT).show()
                    return@ReviewDialog
                }

                val newComment = CommentDomain(
                    id = UUID.randomUUID().toString(),
                    datetime = OffsetDateTime.now().toString(), // Asegúrate de que este formato sea parseable
                    message = message,
                    rating = rating,
                    likes = 0,
                    photo = emptyList(),
                    isVisible = true,
                    author = user,
                    responses = emptyList(),
                    responseTo = null,
                    reports = emptyList(),
                    productDomain = null,
                    restaurantDomain = restaurantDetailViewModel.uiState.value.restaurant
                )

                coroutineScope.launch {
                    try {
                        restaurantDetailViewModel.createReview(newComment)
                        Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to submit review. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}