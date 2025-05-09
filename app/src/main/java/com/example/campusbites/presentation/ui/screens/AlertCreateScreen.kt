package com.example.campusbites.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.DraftAlert
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCreateScreen(
    onBackClick: () -> Unit,
    onAlertCreated: () -> Unit,
    viewModel: AlertsViewModel,
) {
    // Get context for Toast
    val context = LocalContext.current

    // Safely collect states with default values
    val connectivityState by viewModel.connectivityState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val restaurants by viewModel.restaurants.collectAsState(initial = emptyList())
    val draftAlerts by viewModel.draftAlerts.collectAsState(initial = emptyList())

    // Safely access network state with default value
    val isNetworkAvailable = connectivityState.isConnected

    // Track restaurant loading state
    val areRestaurantsLoading = restaurants.isEmpty() && !(uiState.errorMessage?.contains("restaurants") ?: false)

    // Track if we've shown various error toasts to avoid duplicates
    var hasShownRestaurantError by remember { mutableStateOf(false) }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }

    var description by remember { mutableStateOf("") }
    var selectedRestaurantId by remember { mutableStateOf("") }
    var selectedRestaurantName by remember { mutableStateOf("") }
    var isRestaurantMenuExpanded by remember { mutableStateOf(false) }
    var showDraftsList by remember { mutableStateOf(false) }

    // Function to convert technical errors to user-friendly messages
    fun getUserFriendlyErrorMessage(errorMsg: String): String {
        return when {
            errorMsg.contains("network") || errorMsg.contains("timeout") || errorMsg.contains("connection") ->
                "We couldn't connect to the server. Please check your connection and try again."

            errorMsg.contains("restaurants") || errorMsg.contains("failed to fetch restaurants") ->
                "We couldn't get the list of restaurants. Please try again later."

            errorMsg.contains("unauthorized") || errorMsg.contains("authentication") || errorMsg.contains("401") ->
                "Your session has expired. Please log in again."

            errorMsg.contains("permission") || errorMsg.contains("not allowed") || errorMsg.contains("403") ->
                "You don't have permission to perform this action."

            errorMsg.contains("create alert") || errorMsg.contains("failed to create") ->
                "We couldn't create the alert. Please check the data and try again."

            errorMsg.contains("server") || errorMsg.contains("500") ->
                "There's a problem with the server. We're working to fix it. Please try again later."

            errorMsg.contains("draft") && errorMsg.contains("save") ->
                "We couldn't save the draft. Please try again."

            errorMsg.contains("draft") && errorMsg.contains("send") ->
                "We couldn't send the draft. Please try again when you're connected."

            errorMsg.contains("draft") && errorMsg.contains("delete") ->
                "We couldn't delete the draft. Please try again."

            else ->
                "An unexpected error occurred. Please try again later."
        }
    }

    // Set initial state for drafts list visibility after we safely have draftAlerts
    LaunchedEffect(draftAlerts) {
        showDraftsList = draftAlerts.isNotEmpty()
    }

    // Load latest draft if available when first opening the screen
    LaunchedEffect(key1 = true) {
        viewModel.getLatestDraftAlert()
    }

    // Show toast for restaurant error
    LaunchedEffect(restaurants, uiState.errorMessage) {
        if (restaurants.isEmpty() && uiState.errorMessage?.contains("restaurants") == true && !hasShownRestaurantError) {
            val userFriendlyMessage = getUserFriendlyErrorMessage(uiState.errorMessage ?: "")
            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
            hasShownRestaurantError = true
        }
    }

    // Show toast for any new error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            if (errorMsg != lastErrorMessage && !errorMsg.contains("restaurants")) {
                val userFriendlyMessage = getUserFriendlyErrorMessage(errorMsg)
                Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
                lastErrorMessage = errorMsg
            }
        }
    }

    // Show toast for success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { successMsg ->
            val userFriendlyMessage = when {
                successMsg.contains("created successfully") ->
                    "Alert created successfully!"
                successMsg.contains("sent successfully") ->
                    "Alert sent successfully!"
                successMsg.contains("draft saved") ->
                    "Draft saved successfully"
                successMsg.contains("draft deleted") ->
                    "Draft deleted successfully"
                else -> successMsg // Fallback to original message if not one of the known ones
            }

            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_SHORT).show()

            // Only navigate back on successful create/send, not on draft save
            if (successMsg.contains("created successfully") || successMsg.contains("sent successfully")) {
                onAlertCreated()
            }
        }
    }

    // Update fields if a draft is available from getLatestDraftAlert
    LaunchedEffect(key1 = uiState.latestDraftAlert) {
        uiState.latestDraftAlert?.let { draft ->
            if (!showDraftsList) { // Only prefill if not showing drafts list
                description = draft.message
                selectedRestaurantId = draft.restaurantId
                selectedRestaurantName = draft.restaurantName
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AlertTopBar(onBackClick = onBackClick)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Create New Alert",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (draftAlerts.isNotEmpty()) {
                    TextButton(onClick = { showDraftsList = !showDraftsList }) {
                        Text(if (showDraftsList) "Hide Drafts" else "Show Drafts (${draftAlerts.size})")
                    }
                }
            }

            // Network status indicator
            if (!isNetworkAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "You are offline. Your alert will be saved as a draft and you can send it when you are back online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (showDraftsList && draftAlerts.isNotEmpty()) {
                // Show the draft alerts list
                Text(
                    text = "Saved Drafts",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(draftAlerts) { draft ->
                        DraftAlertItem(
                            draft = draft,
                            isNetworkAvailable = isNetworkAvailable,
                            onSendClick = {
                                viewModel.sendDraftAlert(draft.id, draft.message, draft.restaurantId)
                            },
                            onDeleteClick = {
                                viewModel.deleteDraftAlert(draft.id)
                            },
                            onEditClick = {
                                // Fill form with this draft's data and switch to form view
                                description = draft.message
                                selectedRestaurantId = draft.restaurantId
                                selectedRestaurantName = draft.restaurantName
                                showDraftsList = false
                            }
                        )
                    }
                }

                // Add a button to create new alert at the bottom
                Button(
                    onClick = { showDraftsList = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Create New Alert")
                }
            } else {
                // Show alert creation form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        minLines = 3
                    )

                    // Show loading state for restaurants
                    if (areRestaurantsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Loading restaurants...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else if (restaurants.isEmpty()) {
                        // Show error state if no restaurants are loaded (now just a visual indicator, toast already shown)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "Could not load restaurants. Please try again later.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Dropdown para seleccionar restaurante
                        ExposedDropdownMenuBox(
                            expanded = isRestaurantMenuExpanded,
                            onExpandedChange = { isRestaurantMenuExpanded = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.createAlert(description, selectedRestaurantId)
                        },

                        enabled = true, // Logic for enabling this button might be needed (e.g., description not empty, restaurant selected)

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text(if (isNetworkAvailable) "Create Alert" else "Save Draft")
                    }

                    // Show draft alerts button if there are drafts
                    if (draftAlerts.isNotEmpty() && !showDraftsList) {
                        OutlinedButton(
                            onClick = { showDraftsList = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Saved Drafts (${draftAlerts.size})")
                        }
                    }

                    // Add extra padding at bottom for scrolling
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

        }
    }
}

@Composable
fun DraftAlertItem(
    draft: DraftAlert,
    isNetworkAvailable: Boolean,
    onSendClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH) // Changed to Locale.ENGLISH
    val formattedDate = dateFormat.format(Date(draft.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.restaurantName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = draft.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEditClick) {
                    Text("Edit")
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = onSendClick,
                    enabled = isNetworkAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send draft alert"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Send")
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete draft",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}