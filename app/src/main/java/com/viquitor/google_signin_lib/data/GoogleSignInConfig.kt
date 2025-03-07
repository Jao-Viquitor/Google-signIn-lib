package org.viquitor.data

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import java.security.SecureRandom
import java.util.Base64

data class GoogleSignInConfig(
    val webClientId: String,
    val filterByAuthorizedAccounts: Boolean = true,
    val autoSelectEnabled: Boolean = true,
    val nonce: String = generateNonce()
) {
    fun toGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setAutoSelectEnabled(autoSelectEnabled)
            .setNonce(nonce)
            .build()
    }

    companion object {
        private fun generateNonce(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}