package com.ABenhadar.smartlife.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // IMPORTANT : Pour l'émulateur Android, utilisez "10.0.2.2"
    // Pour un téléphone RÉEL, utilisez votre adresse IP WiFi réelle
    // Trouvez votre IP avec: ipconfig (Windows) ou ifconfig (Mac/Linux)
    private const val LOCAL_IP = "192.168.11.171"  // IP WiFi réelle 
    private const val BASE_URL = "http://$LOCAL_IP:8000/"

    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun getApiService(): SmartLifeApiService {
        return retrofit.create(SmartLifeApiService::class.java)
    }
}
