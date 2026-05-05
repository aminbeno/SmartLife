package com.ABenhadar.smartlife.repository

import com.ABenhadar.smartlife.api.RetrofitClient
import com.ABenhadar.smartlife.models.ActivityData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityRepository {
    private val apiService = RetrofitClient.getApiService()

    suspend fun saveActivity(activity: ActivityData): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addActivity(activity)
            if (response.status == "success") {
                Result.success(response.message)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
