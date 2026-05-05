package com.ABenhadar.smartlife.api

import com.ABenhadar.smartlife.models.*
import retrofit2.http.*

interface SmartLifeApiService {

    @GET("health")
    suspend fun checkHealth(): SuccessResponse<Map<String, String>>

    @POST("api/register_user")
    suspend fun registerUser(@Body user: UserData): SuccessResponse<Map<String, String>>

    @GET("api/get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse

    // --- ACTIVITY APIs ---
    @POST("api/activity")
    suspend fun addActivity(@Body activity: ActivityData): SuccessResponse<Map<String, String>>

    @GET("api/activities/{user_id}")
    suspend fun getActivities(@Path("user_id") userId: String): List<ActivityData>

    // --- HABITS APIs ---
    @GET("api/habits/{user_id}")
    suspend fun getHabits(@Path("user_id") userId: String): HabitData

    // --- RECOMMENDATION APIs ---
    @POST("api/recommendation")
    suspend fun addRecommendation(@Body recommendation: RecommendationData): SuccessResponse<Map<String, String>>

    // --- VOICE APIs ---
    @POST("api/voice")
    suspend fun addVoiceLog(@Body voiceLog: VoiceLogData): SuccessResponse<Map<String, String>>
}
