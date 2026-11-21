package com.example.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DuckPalette
import com.example.domain.model.PondNotification
import com.example.domain.model.ThemePreference
import com.example.domain.model.UiState
import com.example.domain.model.Wallet
import com.example.domain.repository.CollectorRepository
import com.example.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioProfileData(
    val profile: CollectorProfile? = null,
    val wallet: Wallet? = null,
    val notifications: List<PondNotification> = emptyList(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val notificationsEnabled: Boolean = true
)

class ProfileViewModel(
    private val currentUserId: String,
    private val collectorRepository: CollectorRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val uiState: StateFlow<UiState<StudioProfileData>> = combine(
        collectorRepository.observeProfile(currentUserId),
        collectorRepository.observeWallet(currentUserId),
        collectorRepository.observeNotifications(currentUserId),
        preferencesRepository.themePreference,
        preferencesRepository.notificationsEnabled
    ) { profile, wallet, notifications, theme, notifEnabled ->
        StudioProfileData(
            profile = profile,
            wallet = wallet,
            notifications = notifications,
            themePreference = theme,
            notificationsEnabled = notifEnabled
        )
    }
        .map<StudioProfileData, UiState<StudioProfileData>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing profile studio", root)
            emit(UiState.Error(root.message ?: "Failed to load profile studio."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun saveAvatarAndProfile(displayName: String, bio: String, avatar: DuckPalette) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = collectorRepository.updateProfileAndAvatar(
                userId = currentUserId,
                displayName = displayName,
                bio = bio,
                avatar = avatar
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Duck avatar & profile saved!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not save profile."
            }
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            preferencesRepository.setThemePreference(preference)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            collectorRepository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead() {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val unreadIds = data.notifications.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            collectorRepository.markAllNotificationsRead(currentUserId, unreadIds)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "ProfileVM"
    }
}
