package com.example.shoter.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class PlayerEliminationRequest(
    val playerId: String,
    val eliminatorId: String,
    val timestamp: Long
)

data class PlayerProfileRequest(
    val playerId: String,
    val images: List<String>, // Base64 encoded images
    val angles: List<String> // "front", "left", "right", "back"
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

interface GameApiService {
    @POST("eliminate-player")
    suspend fun eliminatePlayer(@Body request: PlayerEliminationRequest): Response<ApiResponse>
    
    @POST("create-player-profile")
    suspend fun createPlayerProfile(@Body request: PlayerProfileRequest): Response<ApiResponse>

    @POST("identify-player")
    suspend fun identifyPlayer(@Body request: FaceImageRequest): Response<IdentifyResponse>

    data class FaceImageRequest(val image: String) // base64
    data class IdentifyResponse(val playerId: String, val isNew: Boolean)
}

object GameApi {
    private const val BASE_URL = "http://94.103.9.172:8080/api/"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: GameApiService = retrofit.create(GameApiService::class.java)
}
