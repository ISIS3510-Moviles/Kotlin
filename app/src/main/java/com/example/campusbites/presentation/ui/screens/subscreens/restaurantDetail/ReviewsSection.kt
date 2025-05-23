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
import kotlinx.coroutines.launch // Importa launch
import androidx.compose.runtime.rememberCoroutineScope // Importa rememberCoroutineScope
import android.widget.Toast // Importa Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext // Importa LocalContext

@Composable
fun ReviewsSection(
    restaurantDetailViewModel: RestaurantDetailViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val uiState = restaurantDetailViewModel.uiState.collectAsState().value
    var showDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Firebase.analytics.logEvent("restaurant_reviews_checked", null)
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.reviews) { comment -> CommentCard(comment) }
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
                    datetime = OffsetDateTime.now().toString(),
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