package com.example.campusbites.presentation.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.campusbites.R
import com.example.campusbites.presentation.navigation.AppNavigation
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val oneTapClient: SignInClient by lazy { Identity.getSignInClient(this) }

    private val signInRequest: BeginSignInRequest by lazy {
        BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id)) // Asegúrate de definirlo en `google-services.json`
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CampusBitesTheme {
                AppNavigation()
            }
        }
    }

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    if (idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("SignInSuccess", "Inicio de sesión exitoso")
                                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e("SignInError", "Error en autenticación con Firebase")
                                    Toast.makeText(this, "Error en autenticación", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } catch (e: Exception) {
                    Log.e("SignInError", "Error al obtener credenciales: ${e.message}", e)
                    Toast.makeText(this, "Error al obtener credenciales", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun signIn() {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                signInLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
            }
            .addOnFailureListener { e ->
                Log.e("SignInError", "No se pudo iniciar sesión: ${e.message}", e)
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
            }
    }
}
