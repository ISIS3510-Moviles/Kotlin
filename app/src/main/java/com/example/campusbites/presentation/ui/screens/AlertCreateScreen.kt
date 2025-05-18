package com.example.campusbites.presentation.ui.screens

import android.util.Log
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
    val context = LocalContext.current

    val connectivityState by viewModel.connectivityState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val restaurants by viewModel.restaurants.collectAsState(initial = emptyList())
    val draftAlerts by viewModel.draftAlerts.collectAsState(initial = emptyList())
    val editingDraftIdFromViewModel by viewModel.editingDraftId.collectAsState()

    val isNetworkAvailable = connectivityState.isConnected
    val areRestaurantsLoading = restaurants.isEmpty() && !(uiState.errorMessage?.contains("restaurants") ?: false)

    var hasShownRestaurantError by remember { mutableStateOf(false) }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }

    var description by remember { mutableStateOf("") }
    var selectedRestaurantId by remember { mutableStateOf("") }
    var selectedRestaurantName by remember { mutableStateOf("") }
    var isRestaurantMenuExpanded by remember { mutableStateOf(false) }

    // showDraftsList controla si se ve la lista de drafts o el formulario.
    // Inicia mostrando el formulario si no hay drafts o si estamos editando.
    // Si hay drafts y no estamos editando, inicia mostrando la lista.
    var showDraftsList by remember(editingDraftIdFromViewModel, draftAlerts.size) {
        mutableStateOf(draftAlerts.isNotEmpty() && editingDraftIdFromViewModel == null)
    }

    val isEditingMode = editingDraftIdFromViewModel != null

    // Función para limpiar el formulario localmente. No limpia el estado del ViewModel aquí.
    fun resetLocalFormFields() {
        description = ""
        selectedRestaurantId = ""
        selectedRestaurantName = ""
        Log.d("AlertCreateScreen", "Local form fields reset.")
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("AlertCreateScreen", "DisposableEffect: onDispose, clearing editing state and messages.")
            viewModel.clearEditingDraftState()
            viewModel.clearMessages()
        }
    }

    // Efecto para pre-llenar el formulario cuando `editingDraftIdFromViewModel` cambia y es NO NULO.
    // Y para limpiar el formulario local si `editingDraftIdFromViewModel` se vuelve NULO (p.ej., después de guardar un draft editado).
    LaunchedEffect(editingDraftIdFromViewModel) {
        Log.d("AlertCreateScreen", "LaunchedEffect(editingDraftIdFromViewModel): ID is $editingDraftIdFromViewModel")
        if (editingDraftIdFromViewModel != null) {
            val draftToEdit = draftAlerts.find { it.id == editingDraftIdFromViewModel }
            if (draftToEdit != null) {
                description = draftToEdit.message
                selectedRestaurantId = draftToEdit.restaurantId
                selectedRestaurantName = draftToEdit.restaurantName
                showDraftsList = false // Asegurarse de mostrar el formulario
                Log.d("AlertCreateScreen", "Form pre-filled for editing draft ID: ${draftToEdit.id}")
            } else {

                Log.w("AlertCreateScreen", "Draft to edit (ID: $editingDraftIdFromViewModel) not found in draftAlerts list.")
                resetLocalFormFields() // Limpiar por si acaso
            }
        } else {

            if (!showDraftsList) { // Solo limpiar si el formulario está visible
                resetLocalFormFields()
            }
        }
    }


    fun getUserFriendlyErrorMessage(errorMsg: String): String {
        return when {
            errorMsg.contains("network") || errorMsg.contains("timeout") || errorMsg.contains("connection") ->
                "We couldn't connect to the server. Please check your connection and try again."
            errorMsg.contains("restaurants") || errorMsg.contains("failed to fetch restaurants") ->
                "We couldn't get the list of restaurants. Please try again later."
            errorMsg.contains("draft") && errorMsg.contains("save") || errorMsg.contains("updated successfully") ->
                "We couldn't save the draft. Please try again."
            else -> "An unexpected error occurred. Please try again later."
        }
    }

    LaunchedEffect(restaurants, uiState.errorMessage) {
        if (restaurants.isEmpty() && uiState.errorMessage?.contains("restaurants") == true && !hasShownRestaurantError) {
            val userFriendlyMessage = getUserFriendlyErrorMessage(uiState.errorMessage ?: "")
            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
            hasShownRestaurantError = true
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMsg ->
            if (errorMsg != lastErrorMessage && !errorMsg.contains("restaurants")) {
                val userFriendlyMessage = getUserFriendlyErrorMessage(errorMsg)
                Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_LONG).show()
                lastErrorMessage = errorMsg
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { successMsg ->
            val userFriendlyMessage = when {
                successMsg.contains("created successfully") -> "Alert created successfully!"
                successMsg.contains("sent successfully") -> "Alert sent successfully!"
                successMsg.contains("draft saved successfully") -> "Draft saved successfully"
                successMsg.contains("draft updated successfully") -> "Draft updated successfully"
                successMsg.contains("draft deleted successfully") -> "Draft deleted successfully"
                else -> successMsg
            }
            Toast.makeText(context, userFriendlyMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()

            if (successMsg.contains("created successfully") || successMsg.contains("sent successfully")) {

                onAlertCreated()
            } else if (successMsg.contains("draft saved successfully") || successMsg.contains("draft updated successfully")) {

                showDraftsList = true
            }
        }
    }


    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AlertTopBar(onBackClick = {

                viewModel.clearEditingDraftState()
                onBackClick()
            })

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEditingMode) "Edit Draft Alert" else "Create New Alert",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (!isNetworkAvailable && !isEditingMode && !showDraftsList) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text("You are offline. Your alert will be saved as a draft.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                }
            }

            if (showDraftsList) {
                Text("Saved Drafts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    items(draftAlerts) { draft ->
                        DraftAlertItem(
                            draft = draft,
                            isNetworkAvailable = isNetworkAvailable,
                            onSendClick = { viewModel.sendDraftAlert(draft.id, draft.message, draft.restaurantId) },
                            onDeleteClick = { viewModel.deleteDraftAlert(draft.id) },
                            onEditClick = {
                                viewModel.startEditingDraft(draft)
                            }
                        )
                    }
                }
                if (!isEditingMode) {
                    Button(
                        onClick = {
                            showDraftsList = false
                            resetLocalFormFields()
                            viewModel.clearEditingDraftState()
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Create New Alert Form")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), minLines = 3)

                    if (areRestaurantsLoading) { /* sin cambios */ }
                    else if (restaurants.isEmpty()) { /* sin cambios */ }
                    else {
                        ExposedDropdownMenuBox(expanded = isRestaurantMenuExpanded, onExpandedChange = {isRestaurantMenuExpanded = it}, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            OutlinedTextField(value = selectedRestaurantName, onValueChange = {}, readOnly = true, label = { Text("Restaurant") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRestaurantMenuExpanded)}, modifier = Modifier.fillMaxWidth().menuAnchor())
                            ExposedDropdownMenu(expanded = isRestaurantMenuExpanded, onDismissRequest = {isRestaurantMenuExpanded = false}) {
                                restaurants.forEach { restaurant -> DropdownMenuItem(text = { Text(restaurant.name) }, onClick = {selectedRestaurantName = restaurant.name; selectedRestaurantId = restaurant.id; isRestaurantMenuExpanded = false}) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val formIsValid = description.isNotBlank() && selectedRestaurantId.isNotBlank()
                    val buttonEnabled = formIsValid && !uiState.isLoading

                    Button(
                        onClick = {
                            if (isEditingMode) { // Siempre guardar borrador si se edita
                                viewModel.createOrUpdateDraftAlert(description, selectedRestaurantId)
                            } else if (!isNetworkAvailable) { // Guardar borrador si no hay red y no se edita
                                viewModel.createOrUpdateDraftAlert(description, selectedRestaurantId)
                            }
                            else { // Crear alerta online
                                viewModel.submitOnlineAlert(description, selectedRestaurantId)
                            }
                        },
                        enabled = buttonEnabled,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            if (isEditingMode) "Save Updated Draft"
                            else if (!isNetworkAvailable) "Save Draft"
                            else "Create Alert"
                        )
                    }

                    if (draftAlerts.isNotEmpty() && !showDraftsList && !isEditingMode) {
                        OutlinedButton(
                            onClick = {
                                showDraftsList = true
                                // No necesitamos limpiar campos aquí porque estamos cambiando a la lista.
                                // El estado de edición del ViewModel ya debería ser nulo.
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Saved Drafts (${draftAlerts.size})")
                        }
                    }
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
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)
    val formattedDate = dateFormat.format(Date(draft.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(draft.restaurantName, style = MaterialTheme.typography.titleMedium)
                Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(draft.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEditClick) { Text("Edit") }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onSendClick,
                    enabled = isNetworkAvailable,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send draft alert"); Spacer(modifier = Modifier.width(4.dp)); Text("Send")
                }
                IconButton(onClick = onDeleteClick) { Icon(Icons.Default.Delete, "Delete draft", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}