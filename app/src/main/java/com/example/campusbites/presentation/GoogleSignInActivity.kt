package com.example.campusbites.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.*
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.campusbites.presentation.ui.MainActivity
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.InitialSetupNoNetworkException // IMPORTAR EXCEPCIÓN
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // Usar FirebaseUser directamente
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GoogleSignInActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var auth: FirebaseAuth

    @Inject
    lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInClient: GoogleSignInClient

    // Estados para la UI de Compose
    private var isLoading by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    private var isNetworkErrorForSetup by mutableStateOf(false) // Para distinguir el error de red
    private var pendingUserForRetry by mutableStateOf<FirebaseUser?>(null)


    companion object {
        private const val TAG = "GoogleSignInActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            CampusBitesTheme { // Usa tu tema de la aplicación
                GoogleSignInScreenUI(
                    loading = isLoading,
                    errorMsg = errorMessage,
                    isNetworkError = isNetworkErrorForSetup,
                    onRetry = {
                        if (pendingUserForRetry != null) {
                            // Reintentar checkOrCreateUser con el usuario ya autenticado
                            clearErrorStates()
                            isLoading = true
                            updateUI(pendingUserForRetry)
                        } else {
                            // Reintentar todo el proceso de inicio de sesión
                            clearErrorStates()
                            launchSignInProcess()
                        }
                    }
                )
            }
        }

        // Solo lanzar el proceso de sign-in si no estamos mostrando un error
        // o si no hay un proceso de reintento pendiente.
        if (savedInstanceState == null) { // Evitar relanzar en cambios de config si ya hay un estado
            launchSignInProcess()
        }
    }

    private fun clearErrorStates() {
        errorMessage = null
        isNetworkErrorForSetup = false
        // No limpiar pendingUserForRetry aquí, se usa en el reintento
    }

    private fun launchSignInProcess() {
        isLoading = true
        clearErrorStates()
        pendingUserForRetry = null // Limpiar usuario pendiente al iniciar un nuevo flujo completo

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            launchCredentialManager()
        } else {
            launchGoogleSignInClient()
        }
    }

    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com") // Usa tu string resource
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@GoogleSignInActivity, request)
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "❌ CredentialManager GetCredentialException: ${e.message}", e)
                isLoading = false
                errorMessage = "Failed to get login credential. Please try again."
                // Opcional: Fallback a GoogleSignInClient si es apropiado
                // launchGoogleSignInClient()
            }
        }
    }

    private fun launchGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com") // Usa tu string resource
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "❌ Google Sign-In (legacy) failed: ${e.statusCode}", e)
                isLoading = false
                errorMessage = "Google Sign-In failed because you are not connected to network. Please try accessing to internet and again. (Code: ${e.statusCode})"
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error creating GoogleIdTokenCredential: ${e.message}", e)
                isLoading = false
                errorMessage = "Error processing Google Sign-In. Please try again."
            }
        } else {
            Log.w(TAG, "⚠️ Credential is not of type Google ID! Type: ${credential.type}")
            isLoading = false
            errorMessage = "Unsupported login method. Please try Google Sign-In."
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        isLoading = true // Asegurar que se muestre carga
        clearErrorStates() // Limpiar errores de intentos previos
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ signInWithCredential: success")
                    updateUI(auth.currentUser)
                } else {
                    Log.w(TAG, "❌ signInWithCredential: failure", task.exception)
                    isLoading = false
                    errorMessage = "Firebase authentication failed: ${task.exception?.message ?: "Unknown error"}"
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "✅ Firebase User: ${user.email}, attempting app DB setup.")
            isLoading = true // Indicar que estamos procesando
            clearErrorStates()
            pendingUserForRetry = user // Guardar usuario para posible reintento

            authViewModel.checkOrCreateUser(
                user.uid,
                user.displayName ?: user.email?.substringBefore('@') ?: "User", // Mejor fallback para nombre
                user.email ?: "",
                onSuccess = { appUser ->
                    isLoading = false
                    Log.d(TAG, "✅ App User ${appUser.id} setup success.")
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onFailure = { error ->
                    isLoading = false
                    pendingUserForRetry = user // Mantener el usuario por si se reintenta
                    Log.e(TAG, "❌ checkOrCreateUser failed: ${error.message}", error)
                    if (error is InitialSetupNoNetworkException) {
                        errorMessage = error.message // Mensaje específico de la excepción
                        isNetworkErrorForSetup = true
                    } else {
                        errorMessage = "Failed to set up account: ${error.message ?: "Unknown error"}"
                        isNetworkErrorForSetup = false
                    }
                }
            )
        } else {
            // Este caso se da si firebaseAuthWithGoogle falla y llama a updateUI(null)
            // o si el flujo se interrumpe antes.
            isLoading = false
            if (errorMessage == null) { // Solo poner un error genérico si no hay uno más específico
                errorMessage = "Sign-in failed. Please try again."
            }
            Log.d(TAG, "FirebaseUser is null in updateUI. Error: $errorMessage")
        }
    }

    // signOut no es llamado directamente en este flujo, pero es bueno tenerlo.
    private fun signOut() {
        auth.signOut()
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                Log.d(TAG, "User signed out and credentials cleared.")
                // Aquí podrías reiniciar la actividad o llevar a una pantalla de "Signed Out"
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.message}", e)
            }
        }
        // Actualizar UI si es necesario (ej. si esta actividad permanece)
        isLoading = false
        errorMessage = null
        pendingUserForRetry = null
    }
}

// Composable para la UI de GoogleSignInActivity
@Composable
fun GoogleSignInScreenUI(
    loading: Boolean,
    errorMsg: String?,
    isNetworkError: Boolean, // Para usar el icono correcto
    onRetry: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Signing in, please wait...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else if (errorMsg != null) {
                Icon(
                    imageVector = if (isNetworkError) Icons.Filled.Info else Icons.Filled.Close,
                    contentDescription = "Error Icon",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isNetworkError) "Connection Issue" else "Sign-in Error",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            } else {
                // Estado inicial o intermedio antes de que comience la carga o después de un éxito
                // Podría mostrar un logo o un mensaje de "Bienvenido" brevemente.
                // O si el flujo de `launchSignInProcess` es inmediato, esto podría no verse.
                Text(
                    "Initializing Sign-In...",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}