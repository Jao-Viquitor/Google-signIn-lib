# Google Sign-In Library

**[Versão em Português aqui](#README.pt.md)**

Welcome to `google-signin-lib`, a lightweight, secure, and generic Kotlin library for implementing Google authentication in Android applications using the Credential Manager API. Designed to be simple (1-2 lines of usage), robust, and reusable, this library adheres to SOLID principles, Clean Code, and Clean Architecture, providing seamless integration with "Sign in with Google" while delivering detailed payload data for client-side use and backend validation.

## Table of Contents
1. [Features](#features)
2. [Prerequisites](#prerequisites)
3. [Installation](#installation)
4. [Usage](#usage)
  - [Basic Usage](#basic-usage)
  - [Advanced Usage](#advanced-usage)
  - [Example with Backend Validation](#example-with-backend-validation)
5. [Returned Data](#returned-data)
6. [Configuration](#configuration)
7. [Security](#security)
8. [Troubleshooting](#troubleshooting)
9. [Contributing](#contributing)
10. [License](#license)

---

## Features
- **Simplicity**: Sign in with Google in just 1-2 lines of code.
- **Security**: Uses a nonce to prevent replay attacks and verifies the `idToken` on the client.
- **Flexibility**: Returns comprehensive payload data (email, name, photo, subject ID, issuer, audience).
- **Generic**: No dependency on specific frameworks or proprietary models.
- **Flow Support**: Automatic sign-in, signup, and sign-out capabilities.
- **Clean Architecture**: Follows SOLID and Clean Architecture for maintainability and extensibility.

---

## Prerequisites
Before using the library, the app developer must configure the environment in the [Google Cloud Console](https://console.cloud.google.com/):
1. **Create a Project**:
  - Visit the Google Cloud Console.
  - Create a new project or use an existing one.
2. **Set Up OAuth 2.0**:
  - Navigate to "APIs & Services" > "Credentials".
  - Create an "OAuth Client ID" of type "Web application" to obtain the `webClientId`.
  - Link the `webClientId` to your Android app by providing the package name and SHA-1 fingerprint.
3. **Enable the API**:
  - Activate the "Google Identity API" (if required).
4. **Branding Information**:
  - Add your app’s name, logo, and privacy policy/terms of service URLs in the "OAuth consent screen" section.

You’ll need the generated `webClientId` to configure the library.

---

## Installation
Add the dependency to your module’s `build.gradle` file:
```gradle
dependencies {
    implementation 'com.viquitor:google-signin-lib:1.0.0'
}
```

Sync your project in Android Studio after adding the dependency.

### Internal Dependencies
The library relies on the following dependencies:
- `androidx.credentials:credentials:1.2.0`
- `androidx.credentials:credentials-play-services-auth:1.2.0`
- `com.google.android.libraries.identity.googleid:googleid:1.1.0`
- `com.google.auth:google-auth-library-oauth2-http:1.23.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
- `com.google.code.gson:gson:2.10.1`

---

## Usage

### Basic Usage
Perform a Google sign-in with just 1-2 lines inside a coroutine scope:
```kotlin
val googleSignIn = GoogleSignIn.Builder(context).setWebClientId("YOUR_WEB_CLIENT_ID").build()
val result = googleSignIn.signIn() // Call within a CoroutineScope (e.g., lifecycleScope)

when (result) {
    is SignInResult.Success -> println("Signed in: ${result.email}, Subject ID: ${result.subjectId}")
    is SignInResult.Failure -> println("Error: ${result.errorMessage}")
}
```

### Advanced Usage
Customize the sign-in flow and access all payload data:
```kotlin
import kotlinx.coroutines.launch

val googleSignIn = GoogleSignIn.Builder(context)
    .setWebClientId("YOUR_WEB_CLIENT_ID")
    .setFilterByAuthorizedAccounts(false) // For signup (new users)
    .setAutoSelectEnabled(true)           // Auto-sign-in for returning users
    .build()

lifecycleScope.launch {
    when (val result = googleSignIn.signIn()) {
        is SignInResult.Success -> {
            println("Email: ${result.email}")
            println("Full Name: ${result.displayName}")
            println("Name: ${result.givenName} ${result.familyName}")
            println("Profile Picture: ${result.profilePictureUri}")
            println("Subject ID: ${result.subjectId}")
            println("Issuer: ${result.issuer}")
            println("Audience: ${result.audience}")
            // Backend validation
            validateTokenOnBackend(result.idToken)
        }
        is SignInResult.Failure -> println("Sign-in failed: ${result.errorMessage}")
    }
}

// Sign out
googleSignIn.signOut()
```

### Example with Backend Validation
Send the `idToken` to your backend for definitive validation:
```kotlin
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.http.POST
import retrofit2.http.Body

// API Interface
interface AuthApi {
    @POST("auth/google")
    suspend fun validateGoogleToken(@Body token: Map<String, String>): Response<User>
}

// Usage
val googleSignIn = GoogleSignIn.Builder(context).setWebClientId("YOUR_WEB_CLIENT_ID").build()

lifecycleScope.launch {
    val result = googleSignIn.signIn()
    when (result) {
        is SignInResult.Success -> {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://yourserver.com/")
                .build()
            val authApi = retrofit.create(AuthApi::class.java)
            try {
                val response = authApi.validateGoogleToken(mapOf("idToken" to result.idToken))
                if (response.isSuccessful) {
                    println("User authenticated: ${response.body()}")
                } else {
                    println("Backend validation error")
                }
            } catch (e: Exception) {
                println("Backend call error: ${e.message}")
            }
        }
        is SignInResult.Failure -> println("Sign-in error: ${result.errorMessage}")
    }
}
```

---

## Returned Data
The library returns a `SignInResult`, a sealed class with two states:

### `SignInResult.Success`
Contains the authenticated user’s data:
- `idToken: String`: Token for backend validation.
- `email: String?`: User’s email (nullable).
- `givenName: String?`: User’s given name (nullable).
- `familyName: String?`: User’s family name (nullable).
- `displayName: String?`: User’s full name (nullable).
- `profilePictureUri: String?`: Profile picture URL (nullable).
- `subjectId: String`: Unique user identifier (`sub` from payload).
- `issuer: String`: Token issuer (`iss`, e.g., `https://accounts.google.com`).
- `audience: String`: Token audience (`aud`, typically the `webClientId`).

### `SignInResult.Failure`
Contains the error message:
- `errorMessage: String`: Description of the issue (e.g., "Sign-in failed: Invalid token").

---

## Configuration
The `GoogleSignIn.Builder` class allows customization of the sign-in behavior:
- `setWebClientId(clientId: String)`: Sets the `webClientId` (required).
- `setFilterByAuthorizedAccounts(filter: Boolean)`:
  - `true` (default): Lists only previously authorized accounts.
  - `false`: Allows signup with any Google account.
- `setAutoSelectEnabled(autoSelect: Boolean)`:
  - `true` (default): Enables auto-sign-in for users with a single valid account.
  - `false`: Requires manual account selection.

Example:
```kotlin
val googleSignIn = GoogleSignIn.Builder(context)
    .setWebClientId("YOUR_WEB_CLIENT_ID")
    .setFilterByAuthorizedAccounts(false)
    .setAutoSelectEnabled(false)
    .build()
```

---

## Security
- **Nonce**: A unique nonce is automatically generated for each request, preventing replay attacks.
- **Client-Side Verification**: The library uses `GoogleIdTokenVerifier` to validate the `idToken` signature with Google’s public keys.
- **Backend Validation**: The `idToken` is returned for definitive server-side validation, critical for full security.
- **Encapsulation**: Sensitive data is exposed only through `SignInResult` and not logged.

**Warning**: Client-side validation is preliminary and intended for convenience (e.g., displaying user data). Always validate the `idToken` on the backend to ensure session integrity and authenticity.

---

## Troubleshooting
- **Error: "Sign-in failed: Invalid token"**:
  - Verify that the `webClientId` is correct and linked to your app’s package name and SHA-1.
- **No accounts displayed**:
  - Ensure `filterByAuthorizedAccounts` is set appropriately (`false` for signup).
- **Auto-sign-in not working**:
  - Confirm `setAutoSelectEnabled(true)` is set and there’s a single valid account on the device.
- **Network error during verification**:
  - Ensure the device has an internet connection, as verification requires access to Google’s public keys.

For further assistance, refer to the [Credential Manager official documentation](https://developer.android.com/identity/sign-in/credential-manager-siwg) or open an issue in the repository.

---

## Contributing
Contributions are welcome! Follow these steps:
1. Fork the repository.
2. Create a branch (`git checkout -b feature/new-feature`).
3. Commit your changes (`git commit -m "Add new feature"`).
4. Push to the branch (`git push origin feature/new-feature`).
5. Open a Pull Request.

Please include tests and update the documentation as needed.

---

## License
Distributed under the MIT License. See the `LICENSE` file for more details.
