package com.example.ui.owner

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpecies
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.PromoReward
import com.example.domain.model.WeeklyEvent
import com.example.domain.repository.EventRepository
import com.example.domain.repository.OwnerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerViewModel(
    private val currentUserId: String,
    private val ownerRepository: OwnerRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _showChangingToOwner = MutableStateFlow(false)
    val showChangingToOwner: StateFlow<Boolean> = _showChangingToOwner.asStateFlow()

    private val _showOwnerPanel = MutableStateFlow(false)
    val showOwnerPanel: StateFlow<Boolean> = _showOwnerPanel.asStateFlow()

    private val _lastPromoReward = MutableStateFlow<PromoReward?>(null)
    val lastPromoReward: StateFlow<PromoReward?> = _lastPromoReward.asStateFlow()

    val isOwner: StateFlow<Boolean> = ownerRepository.observeIsOwner(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing owner state", unwrapFirestoreError(err))
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val redeemedCodes: StateFlow<Set<String>> = ownerRepository.observeRedeemedCodes(currentUserId)
        .catch { err ->
            Log.w(TAG, "Error observing redeemed codes", unwrapFirestoreError(err))
            emit(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val allCollectors: StateFlow<List<CollectorProfile>> = isOwner
        .flatMapLatest { owner ->
            if (owner) ownerRepository.observeAllCollectors()
            else flowOf(emptyList())
        }
        .catch { err ->
            Log.w(TAG, "Error observing collectors", unwrapFirestoreError(err))
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val events: StateFlow<List<WeeklyEvent>> = eventRepository.observeEvents()
        .catch { err ->
            Log.w(TAG, "Error observing events for owner panel", unwrapFirestoreError(err))
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun redeemCode(rawCode: String, currentCoins: Long) {
        if (rawCode.isBlank()) {
            _actionMessage.value = "Enter a promo or secret code first!"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = ownerRepository.redeemCode(currentUserId, rawCode, currentCoins)
            _isBusy.value = false
            result.onSuccess { reward ->
                when (reward) {
                    is PromoReward.OwnerUnlockReward -> {
                        _showOwnerPanel.value = false
                        _showChangingToOwner.value = true
                    }
                    is PromoReward.DuckReward,
                    is PromoReward.CoinReward -> {
                        _lastPromoReward.value = reward
                        _actionMessage.value = reward.summary
                    }
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not redeem code."
            }
        }
    }

    fun finishChangingToOwnerTransition() {
        _showChangingToOwner.value = false
        _showOwnerPanel.value = true
    }

    fun openOwnerPanel() {
        if (isOwner.value) {
            _showOwnerPanel.value = true
        }
    }

    fun closeOwnerPanel() {
        _showOwnerPanel.value = false
    }

    fun dismissPromoReward() {
        _lastPromoReward.value = null
    }

    fun grantDuck(
        targetUserId: String,
        species: DuckSpecies,
        customName: String,
        rarityOverride: DuckRarity,
        customPalette: DuckPalette
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val serial = Random.nextLong(1L, 9999L)
            val res = ownerRepository.adminGrantDuck(
                targetUserId = targetUserId,
                species = species,
                customName = customName,
                rarityOverride = rarityOverride,
                customPalette = customPalette,
                serialNumber = serial
            )
            _isBusy.value = false
            res.onSuccess { duck ->
                _actionMessage.value = "Spawned ${rarityOverride.label} ${duck.name} (#${duck.serialNumber})!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to grant duck."
            }
        }
    }

    fun grantCoins(targetUserId: String, amount: Long, isAdd: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = ownerRepository.adminSetOrAddCoins(targetUserId, amount, isAdd)
            _isBusy.value = false
            res.onSuccess { newTotal ->
                _actionMessage.value = if (isAdd) {
                    "Added $amount Grapes (new balance: $newTotal)!"
                } else {
                    "Set wallet balance to $newTotal Grapes!"
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to update Grapes."
            }
        }
    }

    fun sendRoyalGift(targetUserId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val king = DuckSpeciesCatalog.byId("golden_monarch")
            val duckRes = ownerRepository.adminGrantDuck(
                targetUserId = targetUserId,
                species = king,
                customName = "Royal Owner King Ducky",
                rarityOverride = DuckRarity.LEGENDARY,
                customPalette = king.defaultPalette,
                serialNumber = 1L
            )
            val coinRes = ownerRepository.adminSetOrAddCoins(targetUserId, 50_000L, isAdd = true)
            _isBusy.value = false
            if (duckRes.isSuccess && coinRes.isSuccess) {
                _actionMessage.value = "Royal Gift sent: Legendary King Ducky + 50,000 Grapes!"
            } else {
                _actionMessage.value = duckRes.exceptionOrNull()?.message
                    ?: coinRes.exceptionOrNull()?.message
                    ?: "Could not send full Royal Gift."
            }
        }
    }

    fun setBanStatus(targetUserId: String, banned: Boolean, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = ownerRepository.adminSetBanStatus(targetUserId, banned, reason)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (banned) {
                    "Collector suspended from Duck Land."
                } else {
                    "Collector unbanned and welcomed back!"
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to update ban status."
            }
        }
    }

    fun sendAnnouncement(targetUserIds: List<String>, title: String, body: String) {
        if (targetUserIds.isEmpty()) {
            _actionMessage.value = "No target collectors selected."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            var successCount = 0
            var lastError: Throwable? = null
            targetUserIds.forEach { uid ->
                val res = ownerRepository.adminSendAnnouncement(uid, title, body)
                if (res.isSuccess) successCount++ else lastError = res.exceptionOrNull()
            }
            _isBusy.value = false
            if (successCount > 0) {
                _actionMessage.value = "Announcement delivered to $successCount collector(s)!"
            } else {
                _actionMessage.value = lastError?.message ?: "Could not deliver announcement."
            }
        }
    }

    fun activateEvent(eventId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = ownerRepository.adminActivateWeeklyEvent(events.value, eventId)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Active weekly event spotlight updated!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to activate event."
            }
        }
    }

    fun createEvent(
        title: String,
        themeTag: String,
        description: String,
        bonusRarity: String,
        weekNumber: Long,
        activateImmediately: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = ownerRepository.adminCreateWeeklyEvent(
                title = title,
                themeTag = themeTag,
                description = description,
                bonusRarity = bonusRarity,
                weekNumber = weekNumber,
                activateImmediately = activateImmediately,
                existingEvents = events.value
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Created weekly event \"$title\"!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to create weekly event."
            }
        }
    }

    fun updateVersionConfig(
        minRequiredVersionCode: Long,
        latestVersionCode: Long,
        latestVersionName: String,
        updateMessage: String,
        storeUrl: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = ownerRepository.adminUpdateVersionConfig(
                minRequiredVersionCode = minRequiredVersionCode,
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                updateMessage = updateMessage,
                storeUrl = storeUrl
            )
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Remote version gate updated (min code: $minRequiredVersionCode)!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Failed to update version config."
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "OwnerVM"
    }
}
