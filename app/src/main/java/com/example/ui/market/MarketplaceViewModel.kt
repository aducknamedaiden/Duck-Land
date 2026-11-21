package com.example.ui.market

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firestore.unwrapFirestoreError
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DailyMissionCatalog
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.RubberDuck
import com.example.domain.model.TradeOffer
import com.example.domain.model.UiState
import com.example.domain.model.Wallet
import com.example.domain.repository.CollectorRepository
import com.example.domain.repository.MarketplaceRepository
import com.example.domain.repository.MissionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketplaceData(
    val profile: CollectorProfile? = null,
    val wallet: Wallet? = null,
    val myDucks: List<RubberDuck> = emptyList(),
    val activeListings: List<MarketplaceListing> = emptyList(),
    val myListings: List<MarketplaceListing> = emptyList(),
    val tradeOffers: List<TradeOffer> = emptyList()
)

class MarketplaceViewModel(
    private val currentUserId: String,
    private val collectorRepository: CollectorRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val missionRepository: MissionRepository
) : ViewModel() {

    private val todayDateKey: String = DailyMissionCatalog.todayDateKey()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val baseStream = combine(
        collectorRepository.observeProfile(currentUserId),
        collectorRepository.observeWallet(currentUserId),
        collectorRepository.observeMyDucks(currentUserId)
    ) { profile, wallet, ducks -> Triple(profile, wallet, ducks) }

    private val marketStream = combine(
        marketplaceRepository.observeActiveListings(),
        marketplaceRepository.observeMyListings(currentUserId),
        marketplaceRepository.observeTradeOffers(currentUserId)
    ) { active, mine, trades -> Triple(active, mine, trades) }

    val uiState: StateFlow<UiState<MarketplaceData>> = combine(
        baseStream,
        marketStream
    ) { (profile, wallet, ducks), (active, mine, trades) ->
        MarketplaceData(
            profile = profile,
            wallet = wallet,
            myDucks = ducks,
            activeListings = active,
            myListings = mine,
            tradeOffers = trades
        )
    }
        .map<MarketplaceData, UiState<MarketplaceData>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreError(error)
            Log.w(TAG, "Error observing marketplace", root)
            emit(UiState.Error(root.message ?: "Failed to load Marketplace."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    fun buyListing(listing: MarketplaceListing) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val coins = data.wallet?.coins ?: 0L
        val buyerName = data.profile?.displayName ?: "Collector"
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.buyListing(currentUserId, buyerName, coins, listing)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Adopted ${listing.duckName} for ${listing.askingCoins} Grapes!"
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not complete purchase."
            }
        }
    }

    fun cancelListing(listing: MarketplaceListing) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.cancelListing(currentUserId, listing)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "${listing.duckName} returned to your pond."
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not cancel listing."
            }
        }
    }

    fun proposeTrade(
        offeredDuck: RubberDuck,
        listing: MarketplaceListing,
        offeredCoins: Long,
        note: String
    ) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val coins = data.wallet?.coins ?: 0L
        val senderName = data.profile?.displayName ?: "Collector"
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.proposeTrade(
                senderId = currentUserId,
                senderName = senderName,
                senderCoins = coins,
                offeredDuck = offeredDuck,
                listing = listing,
                offeredCoins = offeredCoins,
                note = note
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Trade offer sent to ${listing.sellerName}!"
                launch {
                    missionRepository.recordMissionProgress(
                        userId = currentUserId,
                        dateKey = todayDateKey,
                        missionId = DailyMissionCatalog.MISSION_TRADE_DUCK
                    )
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not send trade offer."
            }
        }
    }

    fun acceptTrade(trade: TradeOffer) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val coins = data.wallet?.coins ?: 0L
        val recipientName = data.profile?.displayName ?: "Collector"
        val requestedListingId = data.myDucks.firstOrNull { it.id == trade.requestedDuckId }?.listingId.orEmpty()
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.acceptTrade(
                recipientId = currentUserId,
                recipientName = recipientName,
                recipientCoins = coins,
                trade = trade,
                requestedDuckListingId = requestedListingId
            )
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Trade complete! ${trade.offeredDuckName} splashed into your pond!"
                launch {
                    missionRepository.recordMissionProgress(
                        userId = currentUserId,
                        dateKey = todayDateKey,
                        missionId = DailyMissionCatalog.MISSION_TRADE_DUCK
                    )
                }
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not accept trade."
            }
        }
    }

    fun declineTrade(trade: TradeOffer) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val recipientName = data.profile?.displayName ?: "Collector"
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.declineTrade(currentUserId, recipientName, trade)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Trade offer declined."
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not decline trade."
            }
        }
    }

    fun cancelTrade(trade: TradeOffer) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val coins = data.wallet?.coins ?: 0L
        viewModelScope.launch {
            _isBusy.value = true
            val result = marketplaceRepository.cancelTrade(currentUserId, coins, trade)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Trade offer cancelled."
            }.onFailure { err ->
                _actionMessage.value = err.message ?: "Could not cancel trade."
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "MarketVM"
    }
}
