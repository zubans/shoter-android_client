package com.example.shoter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoter.network.GameApi
import com.example.shoter.network.PlayerEliminationRequest
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
    val eliminationMessage: String = ""
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
        if (_uiState.value.isPlayerDetected) {
            // Trigger elimination
            eliminateDetectedPlayer()
        }
    }

    fun setTargetingMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isInTargetingMode = enabled)
    }

    fun createPlayerProfile(playerId: String, images: List<String>, angles: List<String>) {
        viewModelScope.launch {
            try {
                val request = PlayerProfileRequest(
                    playerId = playerId,
                    images = images,
                    angles = angles
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

    private fun eliminateDetectedPlayer() {
        viewModelScope.launch {
            try {
                val request = PlayerEliminationRequest(
                    playerId = "detected_player_id", // This should come from player recognition
                    eliminatorId = _uiState.value.currentPlayerId,
                    timestamp = System.currentTimeMillis()
                )
                
                val response = GameApi.service.eliminatePlayer(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        eliminationMessage = "Player eliminated!",
                        isPlayerDetected = false
                    )
                    Log.d("GameViewModel", "Player eliminated successfully")
                } else {
                    Log.e("GameViewModel", "Failed to eliminate player: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error eliminating player", e)
            }
        }
    }

    fun setCurrentPlayerId(playerId: String) {
        _uiState.value = _uiState.value.copy(currentPlayerId = playerId)
    }

    fun clearEliminationMessage() {
        _uiState.value = _uiState.value.copy(eliminationMessage = "")
    }
}
