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
