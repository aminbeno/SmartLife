package com.ABenhadar.smartlife.models

import com.google.gson.annotations.SerializedName

// --- User Models ---
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

// Model for updating FCM token
data class FcmTokenUpdate(val fcmToken: String)

// --- Activities Models ---
data class Location(
    val lat: Double,
    val lng: Double
)

data class ActivityData(
    val user_id: String,
    val type: String,
    val location: Location,
    val timestamp: String? = null,
    val duration: Int,
    val locationName: String? = null // Champ ajouté pour l'affichage du journal
)

// --- Habits Models ---
data class FrequentPlace(
    val name: String,
    val lat: Double,
    val lng: Double,
    val visits: Int
)

data class HabitData(
    val user_id: String,
    val frequent_places: List<FrequentPlace>,
    val active_hours: List<String>
)

// --- Recommendations Models ---
data class RecommendationData(
    val user_id: String,
    val message: String,
    val type: String,
    val created_at: String? = null
)

// --- Voice Logs Models ---
data class VoiceLogData(
    val user_id: String,
    val input: String,
    val response: String,
    val timestamp: String? = null
)

// --- Named Location Model ---
data class NamedLocation(
    @SerializedName("user_id") val user_id: String,
    @SerializedName("name") val name: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

// --- Schedule Models ---
data class ScheduleItem(
    val time: String, // e.g., "08:30"
    val activity_type: String, // e.g., "Walking"
    val location_name: String? = null, // e.g., "Parc Central"
    val lat: Double? = null,
    val lng: Double? = null,
    val duration: Int // minutes
)

data class DaySchedule(
    val day_of_week: String, // e.g., "Monday"
    val items: List<ScheduleItem> = emptyList()
)

data class weeklySchedule(
    @SerializedName("user_id") val user_id: String,
    val days: List<DaySchedule>
)

// --- AI Coach Models ---
data class AIInsightsResponse(
    val recommendations: List<String>,
    val habits: List<String>,
    val prediction: String
)

data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val status: String,
    val response: String
)

// --- Generic Response ---
data class SuccessResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null
)
