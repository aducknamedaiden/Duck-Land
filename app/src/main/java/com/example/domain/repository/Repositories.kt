package com.example.domain.repository

import com.example.domain.model.AppVersionConfig
import com.example.domain.model.CoinClaim
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DailyMissionProgress
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckSpecies
import com.example.domain.model.EventEntry
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.MarketplaceListing
import com.example.domain.model.PondNotification
import com.example.domain.model.RubberDuck
import com.example.domain.model.ThemePreference
import com.example.domain.model.TradeOffer
import com.example.domain.model.Wallet
import com.example.domain.model.WeeklyEvent
import kotlinx.coroutines.flow.Flow

interface CollectorRepository {
    suspend fun ensureInitialized(userId: String, defaultDisplayName: String): Result<Unit>
    fun observeProfile(userId: String): Flow<CollectorProfile?>
    fun observeWallet(userId: String): Flow<Wallet?>
    fun observeMyDucks(userId: String): Flow<List<RubberDuck>>
    fun observeNotifications(userId: String): Flow<List<PondNotification>>
    fun observePendingClaims(userId: String): Flow<List<CoinClaim>>

    fun observeVersionConfig(): Flow<AppVersionConfig?>
    suspend fun ensureVersionConfigSeeded(currentVersionCode: Long, currentVersionName: String): Result<Unit>
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>>
    suspend fun syncLeaderboardEntry(
        userId: String,
        displayName: String,
        avatarConfig: DuckPalette,
        duckCount: Long,
        uniqueSpeciesCount: Long,
        tradesCompleted: Long,
        grapeBalance: Long,
        isAdminAccount: Boolean
    ): Result<Unit>

    suspend fun hatchDuck(
        userId: String,
        currentCoins: Long,
        cost: Long,
        species: DuckSpecies,
        serialNumber: Long
    ): Result<RubberDuck>

    suspend fun claimDailyBonus(userId: String, currentCoins: Long): Result<Unit>
    suspend fun claimPendingCoins(userId: String, currentCoins: Long, claim: CoinClaim): Result<Unit>
    suspend fun updateProfileAndAvatar(
        userId: String,
        displayName: String,
        bio: String,
        avatar: DuckPalette
    ): Result<Unit>

    suspend fun renameDuck(userId: String, duckId: String, newName: String): Result<Unit>
    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit>
    suspend fun markAllNotificationsRead(userId: String, unreadIds: List<String>): Result<Unit>
}

interface MarketplaceRepository {
    fun observeActiveListings(): Flow<List<MarketplaceListing>>
    fun observeMyListings(userId: String): Flow<List<MarketplaceListing>>
    fun observeTradeOffers(userId: String): Flow<List<TradeOffer>>

    suspend fun createListing(
        userId: String,
        profile: CollectorProfile,
        duck: RubberDuck,
        askingCoins: Long,
        wantsTrade: Boolean,
        desiredRarity: String
    ): Result<Unit>

    suspend fun cancelListing(userId: String, listing: MarketplaceListing): Result<Unit>

    suspend fun buyListing(
        buyerId: String,
        buyerName: String,
        buyerCoins: Long,
        listing: MarketplaceListing
    ): Result<Unit>

    suspend fun proposeTrade(
        senderId: String,
        senderName: String,
        senderCoins: Long,
        offeredDuck: RubberDuck,
        listing: MarketplaceListing,
        offeredCoins: Long,
        note: String
    ): Result<Unit>

    suspend fun acceptTrade(
        recipientId: String,
        recipientName: String,
        recipientCoins: Long,
        trade: TradeOffer,
        requestedDuckListingId: String
    ): Result<Unit>

    suspend fun declineTrade(
        recipientId: String,
        recipientName: String,
        trade: TradeOffer
    ): Result<Unit>

    suspend fun cancelTrade(
        senderId: String,
        senderCoins: Long,
        trade: TradeOffer
    ): Result<Unit>
}

interface EventRepository {
    suspend fun ensureSeededEvents(): Result<Unit>
    fun observeEvents(): Flow<List<WeeklyEvent>>
    fun observeEventEntries(eventId: String): Flow<List<EventEntry>>
    suspend fun fetchVotedEntryIds(eventId: String, userId: String, entryIds: List<String>): Set<String>

    suspend fun submitEntry(
        userId: String,
        profile: CollectorProfile,
        eventId: String,
        duck: RubberDuck,
        caption: String
    ): Result<Unit>

    suspend fun voteForEntry(
        userId: String,
        voterName: String,
        eventId: String,
        entry: EventEntry
    ): Result<Long>
}

interface PreferencesRepository {
    val themePreference: Flow<ThemePreference>
    val notificationsEnabled: Flow<Boolean>
    suspend fun setThemePreference(preference: ThemePreference)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}

interface OwnerRepository {
    fun observeIsOwner(userId: String): Flow<Boolean>
    fun observeRedeemedCodes(userId: String): Flow<Set<String>>
    fun observeAllCollectors(): Flow<List<CollectorProfile>>

    suspend fun redeemCode(
        userId: String,
        rawCode: String,
        currentCoins: Long
    ): Result<com.example.domain.model.PromoReward>

    suspend fun adminGrantDuck(
        targetUserId: String,
        species: DuckSpecies,
        customName: String,
        rarityOverride: com.example.domain.model.DuckRarity,
        customPalette: DuckPalette,
        serialNumber: Long
    ): Result<RubberDuck>

    suspend fun adminSetOrAddCoins(
        targetUserId: String,
        amount: Long,
        isAdd: Boolean
    ): Result<Long>

    suspend fun adminSetBanStatus(
        targetUserId: String,
        banned: Boolean,
        reason: String
    ): Result<Unit>

    suspend fun adminSendAnnouncement(
        targetUserId: String,
        title: String,
        body: String
    ): Result<Unit>

    suspend fun adminActivateWeeklyEvent(
        allEvents: List<WeeklyEvent>,
        eventIdToActivate: String
    ): Result<Unit>

    suspend fun adminCreateWeeklyEvent(
        title: String,
        themeTag: String,
        description: String,
        bonusRarity: String,
        weekNumber: Long,
        activateImmediately: Boolean,
        existingEvents: List<WeeklyEvent>
    ): Result<String>

    suspend fun adminUpdateVersionConfig(
        minRequiredVersionCode: Long,
        latestVersionCode: Long,
        latestVersionName: String,
        updateMessage: String,
        storeUrl: String
    ): Result<Unit>
}

interface DuckOracleRepository {
    fun streamOracleAnswer(
        prompt: String,
        myDucks: List<RubberDuck>,
        coins: Long,
        activeEventTitle: String?
    ): Flow<String>

    suspend fun generateDuckLore(
        duck: RubberDuck,
        activeEventTitle: String?
    ): com.example.domain.model.DuckLoreCard

    suspend fun suggestAvatarLook(
        vibePrompt: String
    ): com.example.domain.model.AvatarSuggestion
}

interface MissionRepository {
    fun observeDailyMissions(userId: String, dateKey: String): Flow<List<DailyMissionProgress>>
    suspend fun ensureDailyMissionsInitialized(userId: String, dateKey: String): Result<Unit>
    suspend fun recordMissionProgress(userId: String, dateKey: String, missionId: String): Result<Unit>
    suspend fun claimMissionReward(
        userId: String,
        currentCoins: Long,
        mission: DailyMissionProgress
    ): Result<Unit>
}
