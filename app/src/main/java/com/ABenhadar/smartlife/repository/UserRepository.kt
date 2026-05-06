package com.ABenhadar.smartlife.repository

import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.FcmTokenUpdate
import com.ABenhadar.smartlife.models.UserData
import com.ABenhadar.smartlife.models.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun registerUser(user: UserData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerUser(user)
            if (response.status == "success") {
                Result.success(response.data?.get("id") ?: "")
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

    suspend fun updateUserFCMToken(uid: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateUserFCMToken(uid, FcmTokenUpdate(token))
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
