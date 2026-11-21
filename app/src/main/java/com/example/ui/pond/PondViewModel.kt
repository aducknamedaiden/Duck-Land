package com.example.ui.pond

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.AppVersionConfig
import com.example.domain.model.CoinClaim
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DailyMissionCatalog
import com.example.domain.model.DailyMissionProgress
import com.example.domain.model.DuckPalette
import com.example.domain.model.PondNotification
import com.example.domain.model.RubberDuck
import com.example.domain.model.UiState
import com.example.domain.model.Wallet
import com.example.domain.repository.CollectorRepository
import com.example.domain.repository.EventRepository
import com.example.domain.repository.MarketplaceRepository
import com.example.domain.repository.MissionRepository
import com.example.domain.repository.OwnerRepository
import com.example.domain.repository.PreferencesRepository
import com.example.domain.usecase.HatchDuckUseCase
import com.example.notifications.PondNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PondDashboardData(
    val profile: CollectorProfile? = null,
    val wallet: Wallet? = null,
    val ducks: List<RubberDuck> = emptyList(),
    val notifications: List<PondNotification> = emptyList(),
    val dailyMissions: List<DailyMissionProgress> = emptyList()
)

private data class LeaderboardSyncSnapshot(
    val displayName: String,
    val avatarConfig: Map<String, String>,
    val duckCount: Long,
    val uniqueSpeciesCount: Long,
    val tradesCompleted: Long,
    val grapeBalance: Long,
    val isAdminAccount: Boolean
)

class PondViewModel(
    private val currentUserId: String,
    private val defaultDisplayName: String,
    private val collectorRepository: CollectorRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val eventRepository: EventRepository,
    private val missionRepository: MissionRepository,
    private val ownerRepository: OwnerRepository,
    private val preferencesRepository: PreferencesRepository,
    private val notificationManager: PondNotificationManager,
    private val hatchDuckUseCase: HatchDuckUseCase = HatchDuckUseCase()
) : ViewModel() {

    private val todayDateKey: String = DailyMissionCatalog.todayDateKey()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _hatchedDuck = MutableStateFlow<RubberDuck?>(null)
    val hatchedDuck: StateFlow<RubberDuck?> = _hatchedDuck.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val claimedInFlight = mutableSetOf<String>()
    private var lastSyncedLeaderboard: LeaderboardSyncSnapshot? = null

    val versionConfig: StateFlow<AppVersionConfig?> = collectorRepository.observeVersionConfig()
        .catch { error ->
            Log.w(TAG, "Error observing app version config", unwrapFirestoreError(error))
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val mandatoryUpdateConfig: StateFlow<AppVersionConfig?> = versionConfig
        .map { config ->
            if (config != null && BuildConfig.VERSION_CODE.toLong() < config.minRequiredVersionCode) {
                config
            } else {
                null
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val uiState: StateFlow<UiState<PondDashboardData>> = combine(
        collectorRepository.observeProfile(currentUserId),
        collectorRepository.observeWallet(currentUserId),
        collectorRepository.observeMyDucks(currentUserId),
        collectorRepository.observeNotifications(currentUserId),
        missionRepository.observeDailyMissions(currentUserId, todayDateKey)
    ) { profile, wallet, ducks, notifications, dailyMissions ->
        PondDashboardData(
            profile = profile,
            wallet = wallet,
            ducks = ducks,
            notifications = notifications,
            dailyMissions = dailyMissions
        )
    }
        .map<PondDashboardData, UiState<PondDashboardData>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing pond dashboard", root)
            emit(UiState.Error(root.message ?: "Unable to load your pond right now."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            collectorRepository.ensureInitialized(currentUserId, defaultDisplayName)
            eventRepository.ensureSeededEvents()
            missionRepository.ensureDailyMissionsInitialized(currentUserId, todayDateKey)
            collectorRepository.ensureVersionConfigSeeded(
                currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
                currentVersionName = BuildConfig.VERSION_NAME
            )
        }
        observeClaimsAndNotifications()
        observeLeaderboardSync()
    }

    private fun observeLeaderboardSync() {
        viewModelScope.launch {
            combine(
                uiState,
                ownerRepository.observeIsOwner(currentUserId)
            ) { state, isOwner ->
                val data = (state as? UiState.Success)?.data ?: return@combine null
                val profile = data.profile ?: return@combine null
                val ducks = data.ducks
                val wallet = data.wallet
                LeaderboardSyncSnapshot(
                    displayName = profile.displayName.ifBlank { defaultDisplayName },
                    avatarConfig = profile.avatarConfig,
                    duckCount = ducks.size.toLong(),
                    uniqueSpeciesCount = ducks.map { it.speciesId }.distinct().size.toLong(),
                    tradesCompleted = profile.tradesCompleted,
                    grapeBalance = wallet?.coins ?: 0L,
                    isAdminAccount = isOwner
                )
            }
                .catch { error -> unwrapFirestoreError(error) }
                .collect { snapshot ->
                    if (snapshot != null && snapshot != lastSyncedLeaderboard) {
                        val res = collectorRepository.syncLeaderboardEntry(
                            userId = currentUserId,
                            displayName = snapshot.displayName,
                            avatarConfig = DuckPalette.fromMap(snapshot.avatarConfig),
                            duckCount = snapshot.duckCount,
                            uniqueSpeciesCount = snapshot.uniqueSpeciesCount,
                            tradesCompleted = snapshot.tradesCompleted,
                            grapeBalance = snapshot.grapeBalance,
                            isAdminAccount = snapshot.isAdminAccount
                        )
                        if (res.isSuccess) {
                            lastSyncedLeaderboard = snapshot
                        }
                    }
                }
        }
    }

    private fun observeClaimsAndNotifications() {
        viewModelScope.launch {
            combine(
                collectorRepository.observeWallet(currentUserId),
                collectorRepository.observePendingClaims(currentUserId)
            ) { wallet, claims -> wallet to claims }
                .catch { error -> unwrapFirestoreError(error) }
                .collect { (wallet, claims) ->
                    val coins = wallet?.coins ?: return@collect
                    claims.firstOrNull { !it.claimed && claimedInFlight.add(it.id) }?.let { claim ->
                        val res = collectorRepository.claimPendingCoins(currentUserId, coins, claim)
                        if (res.isSuccess) {
                            _actionMessage.value = "Claimed +${claim.amount} Grapes!"
                        } else {
                            claimedInFlight.remove(claim.id)
                        }
                    }
                }
        }

        viewModelScope.launch {
            collectorRepository.observeNotifications(currentUserId)
                .catch { error -> unwrapFirestoreError(error) }
                .collect { items ->
                    val enabled = preferencesRepository.notificationsEnabled.first()
                    notificationManager.dispatchNewFirestoreNotifications(items, enabled)
                }
        }
    }

    fun hatchEgg(luckyEgg: Boolean) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val currentCoins = data.wallet?.coins ?: 0L
        val cost = if (luckyEgg) 140L else 60L
        if (currentCoins < cost) {
            _actionMessage.value = "You need $cost Grapes to hatch this egg!"
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val species = hatchDuckUseCase.rollSpecies(luckyEgg)
            val serial = hatchDuckUseCase.generateSerialNumber()
            val result = collectorRepository.hatchDuck(currentUserId, currentCoins, cost, species, serial)
            _isBusy.value = false
            result.onSuccess { duck ->
                _hatchedDuck.value = duck
                launch {
                    missionRepository.recordMissionProgress(
                        userId = currentUserId,
                        dateKey = todayDateKey,
                        missionId = DailyMissionCatalog.MISSION_HATCH_EGG
                    )
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not hatch duck egg."
            }
        }
    }

    fun claimDailyMission(mission: DailyMissionProgress) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val currentCoins = data.wallet?.coins ?: 0L
        if (mission.claimed || !mission.isCompleted) return

        viewModelScope.launch {
            _isBusy.value = true
            val result = missionRepository.claimMissionReward(currentUserId, currentCoins, mission)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "+${mission.rewardCoins} Grapes claimed for ${mission.title}!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not claim mission reward."
            }
        }
    }

    fun claimDailySplash() {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val wallet = data.wallet ?: return
        val lastClaimMillis = wallet.lastDailyClaimAt?.toDate()?.time ?: 0L
        val elapsedHours = (System.currentTimeMillis() - lastClaimMillis) / (1000L * 60L * 60L)
        if (lastClaimMillis > 0L && elapsedHours < 12L) {
            _actionMessage.value = "Daily Pond Splash already claimed! Come back in ${12L - elapsedHours}h."
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = collectorRepository.claimDailyBonus(currentUserId, wallet.coins)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "+75 Grapes added from Daily Pond Splash!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not claim daily bonus."
            }
        }
    }

    fun renameDuck(duckId: String, newName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = collectorRepository.renameDuck(currentUserId, duckId, newName)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Duck renamed to ${newName.trim()}!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not rename duck."
            }
        }
    }

    fun listDuckOnMarketplace(
        duck: RubberDuck,
        askingCoins: Long,
        wantsTrade: Boolean,
        desiredRarity: String
    ) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val profile = data.profile ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.createListing(
                userId = currentUserId,
                profile = profile,
                duck = duck,
                askingCoins = askingCoins,
                wantsTrade = wantsTrade,
                desiredRarity = desiredRarity
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "${duck.name} is now live on the Marketplace!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not list duck."
            }
        }
    }

    fun dismissHatchedDuck() {
        _hatchedDuck.value = null
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "PondVM"
    }
}
