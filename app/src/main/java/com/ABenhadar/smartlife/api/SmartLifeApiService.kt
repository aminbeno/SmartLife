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

    @PUT("api/update_user/{uid}")
    suspend fun updateUser(
        @Path("uid") uid: String,
        @Body user: UserData
    ): SuccessResponse<Any>

    @DELETE("api/delete_user/{uid}")
    suspend fun deleteUser(@Path("uid") uid: String): SuccessResponse<Any>
}
