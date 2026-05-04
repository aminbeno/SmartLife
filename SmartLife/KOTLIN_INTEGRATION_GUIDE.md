# SmartLife API - Kotlin Mobile App Integration Guide

## 🚀 Step-by-Step Integration

### Step 1: Add Dependencies to `build.gradle` (Module: app)

```gradle
dependencies {
    // Retrofit for HTTP requests
    implementation 'com.squareup.retrofit2:retrofit:2.10.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.10.0'

    // OkHttp for logging
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

    // Coroutines for async operations
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'

    // Gson for JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'

    // Retrofit Coroutines adapter
    implementation 'com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2'
}
```

### Step 2: Create Data Models

Create a new file: `models/SmartLifeModels.kt`

```kotlin
package com.example.smartlife.models

import com.google.gson.annotations.SerializedName

// Request/Response models
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

data class ErrorResponse(
    val status: String,
    val message: String,
    val code: String? = null
)
```

### Step 3: Create API Service Interface

Create a new file: `api/SmartLifeApiService.kt`

```kotlin
package com.example.smartlife.api

import com.example.smartlife.models.UserData
import com.example.smartlife.models.UserResponse
import com.example.smartlife.models.SuccessResponse
import retrofit2.http.*

interface SmartLifeApiService {

    @GET("health")
    suspend fun checkHealth(): SuccessResponse<Map<String, String>>

    @POST("api/register_user")
    suspend fun registerUser(@Body user: UserData): SuccessResponse<Map<String, String>>

    @GET("api/get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse

    @PUT("api/update_user/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: String,
        @Body user: UserData
    ): SuccessResponse<Any>

    @DELETE("api/delete_user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: String): SuccessResponse<Any>
}
```

### Step 4: Create Retrofit Instance

Create a new file: `api/RetrofitClient.kt`

```kotlin
package com.example.smartlife.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Update this to your backend IP/domain
    private const val BASE_URL = "http://192.168.x.x:8000/"
    // For local testing on emulator:
    // private const val BASE_URL = "http://10.0.2.2:8000/"

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    fun getApiService(): SmartLifeApiService {
        return retrofit.create(SmartLifeApiService::class.java)
    }
}
```

### Step 5: Create Repository

Create a new file: `repository/UserRepository.kt`

```kotlin
package com.example.smartlife.repository

import com.example.smartlife.api.RetrofitClient
import com.example.smartlife.models.UserData
import com.example.smartlife.models.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun registerUser(user: UserData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerUser(user)
            if (response.status == "success") {
                Result.success(response.data?.get("id") as? String ?: "")
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val user = apiService.getUser(uid)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(uid: String, user: UserData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateUser(uid, user)
            if (response.status == "success") {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteUser(uid)
            if (response.status == "success") {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkHealth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            Result.success(response.data?.get("status") ?: "unknown")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Step 6: Create ViewModel

Create a new file: `viewmodel/UserViewModel.kt`

```kotlin
package com.example.smartlife.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartlife.models.UserData
import com.example.smartlife.models.UserResponse
import com.example.smartlife.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    // UI State
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>(null)
    val successMessage: LiveData<String?> = _successMessage

    private val _currentUser = MutableLiveData<UserResponse?>(null)
    val currentUser: LiveData<UserResponse?> = _currentUser

    private val _apiStatus = MutableLiveData<String>("Not checked")
    val apiStatus: LiveData<String> = _apiStatus

    // Check API Health
    fun checkApiHealth() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.checkHealth()
                .onSuccess { status ->
                    _apiStatus.value = "API is ${status.uppercase()}"
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _apiStatus.value = "API Error: ${error.message}"
                    _errorMessage.value = error.message
                    _isLoading.value = false
                }
        }
    }

    // Register New User
    fun registerUser(uid: String, email: String, firstName: String, lastName: String, birthDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val user = UserData(uid, email, firstName, lastName, birthDate)
            repository.registerUser(user)
                .onSuccess { id ->
                    _successMessage.value = "User registered successfully! ID: $id"
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Registration failed"
                    _isLoading.value = false
                }
        }
    }

    // Get User
    fun getUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.getUser(uid)
                .onSuccess { user ->
                    _currentUser.value = user
                    _successMessage.value = "User loaded successfully"
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to fetch user"
                    _isLoading.value = false
                }
        }
    }

    // Update User
    fun updateUser(uid: String, email: String, firstName: String, lastName: String, birthDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val user = UserData(uid, email, firstName, lastName, birthDate)
            repository.updateUser(uid, user)
                .onSuccess { message ->
                    _successMessage.value = message
                    _isLoading.value = false
                    // Reload user data
                    getUser(uid)
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Update failed"
                    _isLoading.value = false
                }
        }
    }

    // Delete User
    fun deleteUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.deleteUser(uid)
                .onSuccess { message ->
                    _successMessage.value = message
                    _currentUser.value = null
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Delete failed"
                    _isLoading.value = false
                }
        }
    }

    // Clear messages
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
```

### Step 7: Use in Activity/Fragment

Example in your Activity:

```kotlin
package com.example.smartlife.ui

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.smartlife.R
import com.example.smartlife.viewmodel.UserViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views
        val uidInput = findViewById<EditText>(R.id.uid_input)
        val emailInput = findViewById<EditText>(R.id.email_input)
        val firstNameInput = findViewById<EditText>(R.id.first_name_input)
        val lastNameInput = findViewById<EditText>(R.id.last_name_input)
        val birthDateInput = findViewById<EditText>(R.id.birth_date_input)

        val registerBtn = findViewById<Button>(R.id.register_btn)
        val getBtn = findViewById<Button>(R.id.get_btn)
        val updateBtn = findViewById<Button>(R.id.update_btn)
        val deleteBtn = findViewById<Button>(R.id.delete_btn)
        val checkHealthBtn = findViewById<Button>(R.id.check_health_btn)

        val statusText = findViewById<TextView>(R.id.status_text)
        val messageText = findViewById<TextView>(R.id.message_text)
        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        // Observe ViewModel
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                messageText.text = "❌ Error: $error"
                messageText.setTextColor(android.graphics.Color.RED)
            }
        }

        viewModel.successMessage.observe(this) { success ->
            if (success != null) {
                messageText.text = "✅ $success"
                messageText.setTextColor(android.graphics.Color.GREEN)
            }
        }

        viewModel.apiStatus.observe(this) { status ->
            statusText.text = status
        }

        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                uidInput.setText(user.uid)
                emailInput.setText(user.email)
                firstNameInput.setText(user.firstName)
                lastNameInput.setText(user.lastName)
                birthDateInput.setText(user.birthDate)
            }
        }

        // Button listeners
        checkHealthBtn.setOnClickListener {
            viewModel.checkApiHealth()
        }

        registerBtn.setOnClickListener {
            val uid = uidInput.text.toString()
            val email = emailInput.text.toString()
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val birthDate = birthDateInput.text.toString()

            if (validateInputs(uid, email, firstName, lastName, birthDate)) {
                viewModel.registerUser(uid, email, firstName, lastName, birthDate)
            }
        }

        getBtn.setOnClickListener {
            val uid = uidInput.text.toString()
            if (uid.isNotEmpty()) {
                viewModel.getUser(uid)
            } else {
                messageText.text = "Please enter a UID"
            }
        }

        updateBtn.setOnClickListener {
            val uid = uidInput.text.toString()
            val email = emailInput.text.toString()
            val firstName = firstNameInput.text.toString()
            val lastName = lastNameInput.text.toString()
            val birthDate = birthDateInput.text.toString()

            if (validateInputs(uid, email, firstName, lastName, birthDate)) {
                viewModel.updateUser(uid, email, firstName, lastName, birthDate)
            }
        }

        deleteBtn.setOnClickListener {
            val uid = uidInput.text.toString()
            if (uid.isNotEmpty()) {
                viewModel.deleteUser(uid)
            } else {
                messageText.text = "Please enter a UID"
            }
        }
    }

    private fun validateInputs(uid: String, email: String, firstName: String, lastName: String, birthDate: String): Boolean {
        return when {
            uid.isEmpty() -> {
                Toast.makeText(this, "UID cannot be empty", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
                false
            }
            firstName.isEmpty() || lastName.isEmpty() -> {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                false
            }
            birthDate.isEmpty() -> {
                Toast.makeText(this, "Birth date cannot be empty", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }
}
```

## ⚙️ Important Configuration

### Update Base URL in RetrofitClient.kt

**For Android Emulator (Local Backend):**

```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"
```

**For Real Device:**

```kotlin
private const val BASE_URL = "http://your-computer-ip:8000/"
// Example: http://192.168.1.100:8000/
```

To find your computer's IP:

```bash
# Windows
ipconfig
# Look for IPv4 Address under your network adapter

# Linux/Mac
ifconfig
```

### AndroidManifest.xml

Add internet permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

For Android 9+, add network security config:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config">
    ...
</application>
```

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">192.168.0.0/16</domain>
    </domain-config>
</network-security-config>
```

## 🧪 Quick Test

To test the connection:

```kotlin
// In your Activity/Fragment
val viewModel: UserViewModel by viewModels()

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Check API health
    viewModel.checkApiHealth()

    // Observe status
    viewModel.apiStatus.observe(this) { status ->
        Log.d("SmartLife", "API Status: $status")
    }
}
```

## 🎯 Common Issues & Solutions

| Issue                     | Solution                                          |
| ------------------------- | ------------------------------------------------- |
| Connection refused        | Check backend is running on port 8000             |
| Use 10.0.2.2 for emulator | This is the special IP for emulator to reach host |
| Wrong Base URL            | Update BASE_URL in RetrofitClient.kt              |
| Timeout errors            | Increase timeout in OkHttpClient builder          |
| CORS errors               | Ensure CORS is enabled in backend (.env file)     |
| SSL certificate error     | Use http:// for local development                 |

---

**Ready to integrate!** 🚀
