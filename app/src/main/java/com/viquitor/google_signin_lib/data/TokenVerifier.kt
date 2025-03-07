package org.viquitor.data

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

object TokenVerifier {
    suspend fun verify(idToken: String, webClientId: String): GoogleIdToken? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()
            val verifier = GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(listOf(webClientId))
                .build()
            verifier.verify(idToken)
        }
    }
}