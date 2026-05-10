package com.ABenhadar.smartlife.api

import com.ABenhadar.smartlife.models.*
import retrofit2.Response
import retrofit2.http.*

interface SmartLifeApiService {

    @GET("health")
    suspend fun checkHealth(): SuccessResponse<Map<String, String>>

    @POST("api/register_user")
    suspend fun registerUser(@Body user: UserData): SuccessResponse<Map<String, String>>

    @GET("api/get_user/{uid}")
    suspend fun getUser(@Path("uid") uid: String): UserResponse

    @PUT("api/update_user/{uid}")
    suspend fun updateUser(@Path("uid") uid: String, @Body user: UserData): SuccessResponse<Map<String, String>>

    @PUT("api/user/{uid}/fcm-token")
    suspend fun updateUserFCMToken(@Path("uid") uid: String, @Body tokenUpdate: FcmTokenUpdate): SuccessResponse<Map<String, String>>

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

    @GET("api/recommendations/{user_id}")
    suspend fun getRecommendations(@Path("user_id") userId: String): List<RecommendationData>

    // --- VOICE APIs ---
    @POST("api/voice")
    suspend fun addVoiceLog(@Body voiceLog: VoiceLogData): SuccessResponse<Map<String, String>>

    // --- NAMED LOCATION API ---
    @POST("api/named_locations")
    suspend fun saveNamedLocation(@Body namedLocation: NamedLocation): Response<SuccessResponse<Any>>

    @GET("api/named_locations/{user_id}")
    suspend fun getNamedLocations(@Path("user_id") userId: String): List<NamedLocation>

    // --- SCHEDULE APIs ---
    @GET("api/schedule/{user_id}")
    suspend fun getWeeklySchedule(@Path("user_id") userId: String): weeklySchedule

    @POST("api/schedule")
    suspend fun saveWeeklySchedule(@Body schedule: weeklySchedule): SuccessResponse<Map<String, String>>

    // --- AI COACH API ---
    @GET("api/ai_insights/{user_id}")
    suspend fun getAIInsights(@Path("user_id") userId: String): AIInsightsResponse
}