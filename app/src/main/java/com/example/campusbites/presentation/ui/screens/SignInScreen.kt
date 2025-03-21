package com.example.campusbites.presentation.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import androidx.navigation.NavController
import com.example.campusbites.R
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.domain.model.UserDomain
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    // Instancia la solicitud de acceso con Google usando GetGoogleIdOption.
    val googleIdOption = GetGoogleIdOption.Builder()
        // Pasa el ID de cliente de “servidor” definido en strings.xml.
        .setServerClientId(context.getString(R.string.default_web_client_id))
        // Sólo muestra las cuentas previamente utilizadas para iniciar sesión.
        .setFilterByAuthorizedAccounts(true)
        .build()

    // Crea la solicitud de Credential Manager.
    val getCredentialRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    // Launcher para iniciar el flujo de Credential Manager.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                coroutineScope.launch {
                    try {
                        val response = credentialManager.getCredential(
                            context = context,
                            request = getCredentialRequest
                        )
                        // Procesa la credencial obtenida.
                        handleSignIn(response.credential)
                    } catch (e: GetCredentialException) {
                        Log.e("SignInScreen", "Error al obtener credenciales: ${e.localizedMessage}")
                        Toast.makeText(context, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: Log.e("SignInScreen", "Datos nulos en el Intent")
        } else {
            Log.e("SignInScreen", "Inicio de sesión cancelado o fallido. Código: ${result.resultCode}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            // Inicia el flujo de inicio de sesión con Credential Manager.
            coroutineScope.launch {
                try {
                    val response = credentialManager.getCredential(
                        context = context,
                        request = getCredentialRequest
                    )
                    handleSignIn(response.credential)
                } catch (e: GetCredentialException) {
                    Log.e("SignInScreen", "Error al iniciar sesión: ${e.localizedMessage}")
                    Toast.makeText(context, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Iniciar sesión con Google")
        }
    }
}

/**
 * Método para procesar la credencial obtenida.
 * Verifica que la credencial sea de tipo Google ID y, de ser así, extrae el token y llama a firebaseAuthWithGoogle.
 */
private fun handleSignIn(credential: Credential) {
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        // Crea el Google ID Token a partir de la información de la credencial.
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        // Intercambia el token por una credencial de Firebase e inicia sesión.
        firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
    } else {
        Log.w("SignInScreen", "Credential is not of type Google ID!")
    }
}

/**
 * Inicializa Firebase Auth en el método onCreate de la actividad:
 *
 * private lateinit var auth: FirebaseAuth
 * // ...
 * auth = Firebase.auth
 *
 * Y en onStart verifica el usuario actual:
 *
 * override fun onStart() {
 *     super.onStart()
 *     val currentUser = auth.currentUser
 *     updateUI(currentUser)
 * }
 */

/**
 * Método para autenticar en Firebase usando el token de Google.
 */
private fun firebaseAuthWithGoogle(idToken: String) {
    val auth = FirebaseAuth.getInstance()
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("SignInScreen", "signInWithCredential: success")
                val user = auth.currentUser
                updateUI(user)
            } else {
                Log.w("SignInScreen", "signInWithCredential: failure", task.exception)
                updateUI(null)
            }
        }
}

/**
 * Actualiza la interfaz de usuario en función del estado del usuario.
 * Implementa este método para navegar a la pantalla principal o mostrar un error.
 */
private fun updateUI(user: FirebaseUser?) {
    // Tu código para actualizar la UI (por ejemplo, navegar a "home_screen" o mostrar un mensaje).
}

/**
 * Método para cerrar sesión y limpiar el estado de credenciales.
 */
private fun signOut(credentialManager: CredentialManager) {
    val auth = FirebaseAuth.getInstance()
    auth.signOut()

    // Cuando un usuario cierra sesión, limpia el estado de credenciales de todos los proveedores.
    // Se recomienda usar un scope de corrutinas (por ejemplo, lifecycleScope) para esta operación.
    // Ejemplo:
    // lifecycleScope.launch {
    //     try {
    //         val clearRequest = ClearCredentialStateRequest()
    //         credentialManager.clearCredentialState(clearRequest)
    //         updateUI(null)
    //     } catch (e: ClearCredentialException) {
    //         Log.e("SignInScreen", "Couldn't clear user credentials: ${e.localizedMessage}")
    //     }
    // }
}
