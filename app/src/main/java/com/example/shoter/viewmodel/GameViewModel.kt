package com.example.shoter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoter.network.GameApi
import com.example.shoter.network.GameApiService
import com.example.shoter.network.PhotoPayload
import com.example.shoter.network.PlayerProfileRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

data class GameUiState(
    val isPlayerDetected: Boolean = false,
    val isInTargetingMode: Boolean = false,
    val playerProfiles: List<String> = emptyList(),
    val currentPlayerId: String = "",
    val eliminationMessage: String = "",
    val isShotInProgress: Boolean = false
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun onPlayerDetected() {
        _uiState.value = _uiState.value.copy(isPlayerDetected = true)
        Log.d("GameViewModel", "Player detected in crosshair!")
    }

    fun onPlayerLost() {
        _uiState.value = _uiState.value.copy(isPlayerDetected = false)
    }

    fun onCrosshairCentered() {
        // No-op: shot is initiated explicitly via UI button
    }

    fun setTargetingMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isInTargetingMode = enabled)
    }

    fun createPlayerProfile(
        playerId: String,
        images: List<String>,
        angles: List<String>,
        photos: List<PhotoPayload> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val request = PlayerProfileRequest(
                    playerId = playerId,
                    images = images,
                    angles = angles,
                    photos = photos.takeIf { it.isNotEmpty() }
                )

                Log.d("HTTP", "Отправляю createPlayerProfile: $playerId")

                val response = GameApi.service.createPlayerProfile(request)
                if (response.isSuccessful) {
                    Log.d("GameViewModel", "Player profile created successfully")
                    // Add to local profiles list
                    val currentProfiles = _uiState.value.playerProfiles.toMutableList()
                    currentProfiles.add(playerId)
                    _uiState.value = _uiState.value.copy(playerProfiles = currentProfiles)
                } else {
                    Log.e("GameViewModel", "Failed to create player profile: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error creating player profile", e)
            }
        }
    }

    fun setCurrentPlayerId(playerId: String) {
        _uiState.value = _uiState.value.copy(currentPlayerId = playerId)
    }

    fun clearEliminationMessage() {
        _uiState.value = _uiState.value.copy(eliminationMessage = "")
    }

    fun startShot() {
        if (_uiState.value.isShotInProgress) return
        _uiState.value = _uiState.value.copy(isShotInProgress = true, eliminationMessage = "")
    }

    fun submitShot(imageBase64: String) {
        viewModelScope.launch {
            try {
                val response = GameApi.service.identifyPlayer(GameApiService.FaceImageRequest(imageBase64))
                if (response.isSuccessful) {
                    val body = response.body()
                    val message = when {
                        body == null -> "Сервер не вернул результат"
                        body.isNew -> "Обнаружен новый игрок: ${body.playerId}"
                        else -> "Попадание по ${body.playerId}"
                    }
                    _uiState.value = _uiState.value.copy(eliminationMessage = message)
                } else if (response.code() == 404) {
                    _uiState.value = _uiState.value.copy(
                        eliminationMessage = "Профиль не найден"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        eliminationMessage = "Ошибка распознавания: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    eliminationMessage = "Сбой распознавания: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isShotInProgress = false)
            }
        }
    }

    fun onShotFailed(message: String) {
        _uiState.value = _uiState.value.copy(
            eliminationMessage = message,
            isShotInProgress = false
        )
    }
}
