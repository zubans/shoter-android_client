package com.example.shoter.network

import com.example.shoter.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class PhotoPayload(
    val angle: String,
    val original: String,
    val face: String? = null,
    val body: String? = null,
    val capturedAt: Long,
    val width: Int,
    val height: Int
)

data class PlayerProfileRequest(
    val playerId: String,
    val images: List<String>, // Base64 encoded face crops (legacy field)
    val angles: List<String>, // "front", "left", "right", "back"
    val photos: List<PhotoPayload>? = null
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

interface GameApiService {
    @POST("create-player-profile")
    suspend fun createPlayerProfile(@Body request: PlayerProfileRequest): Response<ApiResponse>

    @POST("identify-player")
    suspend fun identifyPlayer(@Body request: FaceImageRequest): Response<IdentifyResponse>

    data class FaceImageRequest(val image: String) // base64
    data class IdentifyResponse(val playerId: String, val isNew: Boolean)
}

object GameApi {
    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: GameApiService = retrofit.create(GameApiService::class.java)
}
