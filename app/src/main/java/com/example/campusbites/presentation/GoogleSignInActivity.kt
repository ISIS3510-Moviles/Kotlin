package com.example.campusbites.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.*
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
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

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            launchCredentialManager()
        } else {
            launchGoogleSignInClient()
        }
    }

    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId("119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@GoogleSignInActivity,
                    request = request
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "❌ Couldn't retrieve user's credentials: ${e.localizedMessage}")
                // Puedes hacer fallback aquí si quieres
            }
        }
    }

    private fun launchGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("119422410652-bha101ea66rinavdp34l9361fksgnqlp.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "❌ Google sign in failed", e)
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "⚠️ Credential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w(TAG, "❌ signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun signOut() {
        auth.signOut()
        lifecycleScope.launch {
            try {
                val clearRequest = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(clearRequest)
                updateUI(null)
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
            }
        }
    }

    private fun updateUI(user: com.google.firebase.auth.FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "✅ Usuario autenticado con Firebase: ${user.email}")

            authViewModel.checkOrCreateUser(
                user.uid,
                user.displayName ?: "",
                user.email ?: "",
                onSuccess = {
                    Log.d(TAG, "✅ Usuario registrado exitosamente en la base de datos")

                    runOnUiThread {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent().apply { putExtra("login_success", true) }
                        setResult(RESULT_OK, intent)
                        finish()
                    }

                    signOut()
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Error al registrar el usuario: ${error.message}")
                    runOnUiThread {
                        Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
