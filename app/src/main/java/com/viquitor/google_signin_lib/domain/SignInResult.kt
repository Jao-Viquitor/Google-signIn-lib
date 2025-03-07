package org.viquitor.domain

sealed class SignInResult {
    data class Success(
        val idToken: String,           // Para validação no backend
        val email: String?,            // E-mail do usuário
        val givenName: String?,        // Nome dado
        val familyName: String?,       // Sobrenome
        val displayName: String?,      // Nome completo
        val profilePictureUri: String?,// URL da foto de perfil
        val subjectId: String,         // ID único do usuário (sub)
        val issuer: String,            // Emissor (iss)
        val audience: String           // Audiência (aud)
    ) : SignInResult()

    data class Failure(val errorMessage: String) : SignInResult()
}