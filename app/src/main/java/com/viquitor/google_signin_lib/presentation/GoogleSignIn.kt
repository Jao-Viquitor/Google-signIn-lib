package org.viquitor.presentation

import android.content.Context
import androidx.credentials.CredentialManager
import org.viquitor.data.GoogleSignInConfig
import org.viquitor.domain.SignInResult
import org.viquitor.domain.SignInWithGoogleUseCase

class GoogleSignIn private constructor(
    private val context: Context,
    private val config: GoogleSignInConfig
) {
    private val credentialManager = CredentialManager.create(context)
    private val signInUseCase = SignInWithGoogleUseCase(credentialManager, config)

    suspend fun signIn(): SignInResult = signInUseCase(context)

    suspend fun signOut() {
        credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
    }

    class Builder(private val context: Context) {
        private var webClientId: String? = null
        private var filterByAuthorizedAccounts = true
        private var autoSelectEnabled = true

        fun setWebClientId(clientId: String) = apply { this.webClientId = clientId }
        fun setFilterByAuthorizedAccounts(filter: Boolean) = apply { this.filterByAuthorizedAccounts = filter }
        fun setAutoSelectEnabled(autoSelect: Boolean) = apply { this.autoSelectEnabled = autoSelect }

        fun build(): GoogleSignIn {
            val clientId = webClientId ?: throw IllegalStateException("ID de Cliente Web é obrigatório")
            val config = GoogleSignInConfig(clientId, filterByAuthorizedAccounts, autoSelectEnabled)
            return GoogleSignIn(context, config)
        }
    }
}