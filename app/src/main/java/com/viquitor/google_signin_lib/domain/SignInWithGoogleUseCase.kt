package org.viquitor.domain

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import org.viquitor.data.GoogleSignInConfig
import org.viquitor.data.TokenVerifier


class SignInWithGoogleUseCase(
    private val credentialManager: CredentialManager,
    private val config: GoogleSignInConfig
) {
    suspend operator fun invoke(context: Context): SignInResult {
        return try {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(config.toGoogleIdOption())
                .build()
            val result = credentialManager.getCredential(context, request)
            parseCredential(result)
        } catch (e: Exception) {
            SignInResult.Failure("Falha no login: ${e.message ?: "Erro desconhecido"}")
        }
    }

    private suspend fun parseCredential(result: GetCredentialResponse): SignInResult {
        val credential = result.credential
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleCredential.idToken
                val verifiedToken = TokenVerifier.verify(idToken, config.webClientId)
                    ?: return SignInResult.Failure("Token inválido ou não verificado")

                val payload = verifiedToken.payload
                SignInResult.Success(
                    idToken = idToken,
                    email = payload.email ?: googleCredential.id,
                    givenName = payload["given_name"] as? String ?: googleCredential.givenName,
                    familyName = payload["family_name"] as? String ?: googleCredential.familyName,
                    displayName = googleCredential.displayName,
                    profilePictureUri = googleCredential.profilePictureUri?.toString(),
                    subjectId = payload.subject,
                    issuer = payload.issuer,
                    audience = payload.audience.toString()
                )
            } catch (e: GoogleIdTokenParsingException) {
                SignInResult.Failure("Erro ao parsear o token: ${e.message}")
            }
        } else {
            SignInResult.Failure("Tipo de credencial inesperado")
        }
    }
}