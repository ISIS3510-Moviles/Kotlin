package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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

    // Estados para los campos editables
    val editableName by vendorViewModel.editableName.collectAsState()
    val editableDescription by vendorViewModel.editableDescription.collectAsState()
    val editableAddress by vendorViewModel.editableAddress.collectAsState()
    val editablePhone by vendorViewModel.editablePhone.collectAsState()
    val editableEmail by vendorViewModel.editableEmail.collectAsState()
    val editableOpeningTime by vendorViewModel.editableOpeningTime.collectAsState()
    val editableClosingTime by vendorViewModel.editableClosingTime.collectAsState()
    val editableOpensWeekends by vendorViewModel.editableOpensWeekends.collectAsState()
    val editableOpensHolidays by vendorViewModel.editableOpensHolidays.collectAsState()
    val editableIsActive by vendorViewModel.editableIsActive.collectAsState()

    // Estados para el proceso de guardado
    val isSaving by vendorViewModel.isSaving.collectAsState()
    val saveSuccess by vendorViewModel.saveSuccess.collectAsState()
    val saveErrorMessage by vendorViewModel.saveErrorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vendorRestaurantId) {
        if (!vendorRestaurantId.isNullOrBlank()) {
            vendorViewModel.loadVendorRestaurant(vendorRestaurantId)
        }
    }

    // Mostrar Snackbar para el estado de guardado
    LaunchedEffect(saveSuccess, saveErrorMessage) {
        when {
            saveSuccess == true -> {
                snackbarHostState.showSnackbar(
                    message = "Changes saved successfully!",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                vendorViewModel.resetSaveStatus()
            }
            saveSuccess == false -> {
                snackbarHostState.showSnackbar(
                    message = saveErrorMessage ?: "Failed to save changes.",
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                vendorViewModel.resetSaveStatus()
            }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
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
                    EditableRestaurantDetailsSection(
                        name = editableName,
                        onNameChange = { vendorViewModel.editableName.value = it },
                        description = editableDescription,
                        onDescriptionChange = { vendorViewModel.editableDescription.value = it },
                        address = editableAddress,
                        onAddressChange = { vendorViewModel.editableAddress.value = it },
                        phone = editablePhone,
                        onPhoneChange = { vendorViewModel.editablePhone.value = it },
                        email = editableEmail,
                        onEmailChange = { vendorViewModel.editableEmail.value = it },
                        openingTime = editableOpeningTime,
                        onOpeningTimeChange = { vendorViewModel.editableOpeningTime.value = it },
                        closingTime = editableClosingTime,
                        onClosingTimeChange = { vendorViewModel.editableClosingTime.value = it },
                        opensWeekends = editableOpensWeekends,
                        onOpensWeekendsChange = { vendorViewModel.editableOpensWeekends.value = it },
                        opensHolidays = editableOpensHolidays,
                        onOpensHolidaysChange = { vendorViewModel.editableOpensHolidays.value = it },
                        isActive = editableIsActive,
                        onIsActiveChange = { vendorViewModel.editableIsActive.value = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { vendorViewModel.saveChanges() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }

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

@OptIn(ExperimentalMaterial3Api::class) // Necesario para TimePicker
@Composable
private fun EditableRestaurantDetailsSection(
    name: String, onNameChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    address: String, onAddressChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    openingTime: String, onOpeningTimeChange: (String) -> Unit, // Ahora es HH:mm
    closingTime: String, onClosingTimeChange: (String) -> Unit, // Ahora es HH:mm
    opensWeekends: Boolean, onOpensWeekendsChange: (Boolean) -> Unit,
    opensHolidays: Boolean, onOpensHolidaysChange: (Boolean) -> Unit,
    isActive: Boolean, onIsActiveChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Restaurant Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Campos de texto editables
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Restaurant Name") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            minLines = 3
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        // --- Time Pickers para Opening Time y Closing Time ---
        // El formato de la UI ya es HH:mm, no necesitamos un formateador aquí para mostrarlo.
        // Pero sí para parsear la hora inicial del TimePicker.
        val uiTimeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

        // Opening Time
        var showOpeningTimePicker by remember { mutableStateOf(false) }
        TimeInputRow(
            label = "Opening Time",
            time = openingTime, // openingTime ya viene en HH:mm del ViewModel
            onClick = { showOpeningTimePicker = true }
        )
        if (showOpeningTimePicker) {
            // Intentar parsear la hora actual para inicializar el TimePicker
            val initialOpeningLocalTime = try {
                LocalTime.parse(openingTime, uiTimeFormatter)
            } catch (e: DateTimeParseException) {
                LocalTime.of(0, 0) // Valor por defecto si no se puede parsear
            }

            TimePickerDialog(
                onDismissRequest = { showOpeningTimePicker = false },
                onConfirm = { hour, minute ->
                    val newTime = LocalTime.of(hour, minute).format(uiTimeFormatter)
                    onOpeningTimeChange(newTime)
                    showOpeningTimePicker = false
                },
                initialHour = initialOpeningLocalTime.hour,
                initialMinute = initialOpeningLocalTime.minute
            )
        }

        // Closing Time
        var showClosingTimePicker by remember { mutableStateOf(false) }
        TimeInputRow(
            label = "Closing Time",
            time = closingTime, // closingTime ya viene en HH:mm del ViewModel
            onClick = { showClosingTimePicker = true }
        )
        if (showClosingTimePicker) {
            val initialClosingLocalTime = try {
                LocalTime.parse(closingTime, uiTimeFormatter)
            } catch (e: DateTimeParseException) {
                LocalTime.of(0, 0)
            }

            TimePickerDialog(
                onDismissRequest = { showClosingTimePicker = false },
                onConfirm = { hour, minute ->
                    val newTime = LocalTime.of(hour, minute).format(uiTimeFormatter)
                    onClosingTimeChange(newTime)
                    showClosingTimePicker = false
                },
                initialHour = initialClosingLocalTime.hour,
                initialMinute = initialClosingLocalTime.minute
            )
        }
        // --- Fin de Time Pickers ---

        // Campos booleanos editables con Switch
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Opens Weekends", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = opensWeekends, onCheckedChange = onOpensWeekendsChange)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Opens Holidays", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = opensHolidays, onCheckedChange = onOpensHolidaysChange)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Is Active", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isActive, onCheckedChange = onIsActiveChange)
        }
    }
}

@Composable
private fun TimeInputRow(
    label: String,
    time: String, // Este 'time' ya debería venir en HH:mm del ViewModel
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(time.ifBlank { "Select Time" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timeState)
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timeState.hour, timeState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}