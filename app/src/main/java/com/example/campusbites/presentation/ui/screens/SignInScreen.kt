package com.example.campusbites.presentation.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.CreateUserUseCase
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignInScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val firebaseAuth = FirebaseAuth.getInstance()

    val oneTapClient: SignInClient = Identity.getSignInClient(context)
    val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId("119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com")
                .setFilterByAuthorizedAccounts(false)
                .build()
        )
        .setAutoSelectEnabled(true)
        .build()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val firebaseUser = firebaseAuth.currentUser
                            firebaseUser?.let { user ->
                                authViewModel.setUser(user) // Guardar usuario en ViewModel
                                authViewModel.checkOrCreateUser(
                                    userId = user.uid,
                                    userName = user.displayName ?: "Usuario",
                                    userEmail = user.email ?: "",
                                    onSuccess = { navController.navigate("home_screen") },
                                    onFailure = {
                                        Toast.makeText(context, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            val errorMessage = authResult.exception?.localizedMessage ?: "Error desconocido"
                            Log.e("AuthError", "Error al iniciar sesión: $errorMessage")
                            Toast.makeText(context, "Error al iniciar sesión: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                    launcher.launch(intentSenderRequest)
                }
                .addOnFailureListener {
                    Toast.makeText(context, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                }
        }) {
            Text("Iniciar sesión con Google")
        }
    }
}


