package com.example.ui.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AvatarSuggestion
import com.example.domain.model.DuckLoreCard
import com.example.domain.model.RubberDuck
import com.example.domain.repository.DuckOracleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface OracleChatUiState {
    data object Idle : OracleChatUiState
    data object Loading : OracleChatUiState
    data class Streaming(val prompt: String, val text: String) : OracleChatUiState
    data class Success(val prompt: String, val text: String) : OracleChatUiState
    data class Error(val message: String) : OracleChatUiState
}

sealed interface DuckLoreUiState {
    data object Idle : DuckLoreUiState
    data object Loading : DuckLoreUiState
    data class Success(val lore: DuckLoreCard) : DuckLoreUiState
    data class Error(val message: String) : DuckLoreUiState
}

sealed interface AvatarStylistUiState {
    data object Idle : AvatarStylistUiState
    data object Loading : AvatarStylistUiState
    data class Success(val suggestion: AvatarSuggestion) : AvatarStylistUiState
    data class Error(val message: String) : AvatarStylistUiState
}

class DuckOracleViewModel(
    private val oracleRepository: DuckOracleRepository
) : ViewModel() {

    private val _chatState = MutableStateFlow<OracleChatUiState>(OracleChatUiState.Idle)
    val chatState: StateFlow<OracleChatUiState> = _chatState.asStateFlow()

    private val _loreState = MutableStateFlow<DuckLoreUiState>(DuckLoreUiState.Idle)
    val loreState: StateFlow<DuckLoreUiState> = _loreState.asStateFlow()

    private val _stylistState = MutableStateFlow<AvatarStylistUiState>(AvatarStylistUiState.Idle)
    val stylistState: StateFlow<AvatarStylistUiState> = _stylistState.asStateFlow()

    private val _showOracleModal = MutableStateFlow(false)
    val showOracleModal: StateFlow<Boolean> = _showOracleModal.asStateFlow()

    private val _initialTab = MutableStateFlow(0)
    val initialTab: StateFlow<Int> = _initialTab.asStateFlow()

    private val _selectedLoreDuckId = MutableStateFlow<String?>(null)
    val selectedLoreDuckId: StateFlow<String?> = _selectedLoreDuckId.asStateFlow()

    fun openOracle(tab: Int = 0, duckForLore: RubberDuck? = null, activeEventTitle: String? = null) {
        _initialTab.value = tab
        if (duckForLore != null) {
            _selectedLoreDuckId.value = duckForLore.id
            generateLoreForDuck(duckForLore, activeEventTitle)
        }
        _showOracleModal.value = true
    }

    fun closeOracle() {
        _showOracleModal.value = false
    }

    fun askOracle(
        prompt: String,
        myDucks: List<RubberDuck>,
        coins: Long,
        activeEventTitle: String?
    ) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty()) {
            _chatState.value = OracleChatUiState.Error("Please enter a question for the Pond Oracle.")
            return
        }
        viewModelScope.launch {
            _chatState.value = OracleChatUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    oracleRepository.streamOracleAnswer(cleanPrompt, myDucks, coins, activeEventTitle)
                        .collect { chunk ->
                            accumulated.append(chunk)
                            _chatState.value = OracleChatUiState.Streaming(cleanPrompt, accumulated.toString())
                        }
                }
                if (accumulated.isNotBlank()) {
                    _chatState.value = OracleChatUiState.Success(cleanPrompt, accumulated.toString())
                } else {
                    _chatState.value = OracleChatUiState.Error("Gemini returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Oracle stream timed out", e)
                _chatState.value = OracleChatUiState.Error("Pond Oracle timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Oracle stream failed", e)
                _chatState.value = OracleChatUiState.Error(e.localizedMessage ?: "Pond Oracle request failed.")
            }
        }
    }

    fun generateLoreForDuck(duck: RubberDuck, activeEventTitle: String?) {
        _selectedLoreDuckId.value = duck.id
        viewModelScope.launch {
            _loreState.value = DuckLoreUiState.Loading
            try {
                val lore = withTimeout(60_000L) {
                    oracleRepository.generateDuckLore(duck, activeEventTitle)
                }
                _loreState.value = DuckLoreUiState.Success(lore)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Duck lore generation timed out", e)
                _loreState.value = DuckLoreUiState.Error("Duck lore generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Duck lore generation failed", e)
                _loreState.value = DuckLoreUiState.Error(e.localizedMessage ?: "Could not generate duck lore.")
            }
        }
    }

    fun suggestAvatarLook(vibePrompt: String) {
        val clean = vibePrompt.trim()
        if (clean.isEmpty()) {
            _stylistState.value = AvatarStylistUiState.Error("Describe a vibe or outfit theme first!")
            return
        }
        viewModelScope.launch {
            _stylistState.value = AvatarStylistUiState.Loading
            try {
                val suggestion = withTimeout(60_000L) {
                    oracleRepository.suggestAvatarLook(clean)
                }
                _stylistState.value = AvatarStylistUiState.Success(suggestion)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Avatar stylist timed out", e)
                _stylistState.value = AvatarStylistUiState.Error("Avatar stylist timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Avatar stylist failed", e)
                _stylistState.value = AvatarStylistUiState.Error(e.localizedMessage ?: "Could not style avatar.")
            }
        }
    }

    private companion object {
        const val TAG = "DuckOracleVM"
    }
}
