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
    val duration: Int
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


// --- Generic Response ---
data class SuccessResponse<T>(
    val status: String,
    val message: String,
    val data: T? = null
)
