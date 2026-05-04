package com.ABenhadar.smartlife.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ABenhadar.smartlife.models.UserData
import com.ABenhadar.smartlife.models.UserResponse
import com.ABenhadar.smartlife.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val repository = UserRepository()

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>(null)
    val successMessage: LiveData<String?> = _successMessage

    private val _currentUser = MutableLiveData<UserResponse?>(null)
    val currentUser: LiveData<UserResponse?> = _currentUser

    private val _apiStatus = MutableLiveData<String>("Not checked")
    val apiStatus: LiveData<String> = _apiStatus

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

    fun registerUser(uid: String, email: String, firstName: String, lastName: String, birthDate: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val user = UserData(uid, email, firstName, lastName, birthDate)
            repository.registerUser(user)
                .onSuccess { id ->
                    _successMessage.value = "User registered successfully in DB"
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "DB Registration failed"
                    _isLoading.value = false
                }
        }
    }

    fun getUser(uid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.getUser(uid)
                .onSuccess { user ->
                    _currentUser.value = user
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to fetch user"
                    _isLoading.value = false
                }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
