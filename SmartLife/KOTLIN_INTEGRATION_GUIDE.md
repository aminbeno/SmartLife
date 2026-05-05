# SmartLife API - Kotlin Mobile App Integration Guide

## 🚀 Step-by-Step Integration

### Step 1: Hybrid Authentication Flow
The app uses **Firebase Authentication** for account security and **FastAPI/MongoDB** for user profile data.

**Registration Process:**
1. Create user in Firebase using `createUserWithEmailAndPassword`.
2. On success, take the `uid` from Firebase.
3. Call the FastAPI endpoint `/api/register_user` with the `uid` and profile data.

### Step 2: Add Dependencies to `app/build.gradle.kts`

```kotlin
dependencies {
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
```

### Step 3: Data Models

File: `com.ABenhadar.smartlife.models.SmartLifeModels.kt`

```kotlin
package com.ABenhadar.smartlife.models

import com.google.gson.annotations.SerializedName

data class UserData(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String
)

data class UserResponse(
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    @SerializedName("_id")
    val id: String? = null
)

data class SuccessResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null
)
```

### Step 4: API Service Interface

File: `com.ABenhadar.smartlife.api.SmartLifeApiService.kt`

```kotlin
package com.ABenhadar.smartlife.api

import com.ABenhadar.smartlife.models.UserData
import com.ABenhadar.smartlife.models.UserResponse
import com.ABenhadar.smartlife.models.SuccessResponse
import retrofit2.http.*

interface SmartLifeApiService {
    @GET("health")
    suspend fun checkHealth(): SuccessResponse<Map<String, String>>

    @POST("api/register_user")
    suspend fun registerUser(@Body user: UserData): SuccessResponse<Map<String, String>>

    @GET("api/get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse
}
```

### Step 5: Configuration (RetrofitClient.kt)

For local development with the Android Emulator:
```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

### Step 6: Registration Implementation (RegisterActivity.kt)

```kotlin
// 1. Firebase Auth
auth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
            val user = auth.currentUser
            // 2. Sync with Backend
            user?.let {
                viewModel.registerUser(
                    uid = it.uid,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate
                )
            }
        }
    }
```

---
**Note:** Ensure `android:networkSecurityConfig` is set in `AndroidManifest.xml` to allow cleartext traffic to `10.0.2.2` during development.