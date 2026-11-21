package com.example.ui.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.example.BuildConfig
import com.example.R
import com.example.data.ai.FirebaseDuckOracleRepository
import com.example.data.firestore.FirestoreCollectorRepository
import com.example.data.firestore.FirestoreEventRepository
import com.example.data.firestore.FirestoreMarketplaceRepository
import com.example.data.firestore.FirestoreMissionRepository
import com.example.data.firestore.FirestoreOwnerRepository
import com.example.data.preferences.ThemePreferencesRepository
import com.example.domain.model.AppVersionConfig
import com.example.domain.model.PondNotification
import com.example.domain.model.UiState
import com.example.notifications.PondNotificationManager
import com.example.ui.ai.DuckOracleSheet
import com.example.ui.ai.DuckOracleViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.GrapeAppBackground
import com.example.ui.components.WindowWidthClass
import com.example.ui.events.EventsScreen
import com.example.ui.events.EventsViewModel
import com.example.ui.leaderboard.LeaderboardScreen
import com.example.ui.leaderboard.LeaderboardViewModel
import com.example.ui.market.MarketplaceScreen
import com.example.ui.market.MarketplaceViewModel
import com.example.ui.owner.ChangingToOwnerScreen
import com.example.ui.owner.OwnerPanelScreen
import com.example.ui.owner.OwnerViewModel
import com.example.ui.owner.PromoRewardDialog
import com.example.ui.pond.PondScreen
import com.example.ui.pond.PondViewModel
import com.example.ui.profile.ProfileAvatarScreen
import com.example.ui.profile.ProfileViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class DuckLandDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    POND("pond", R.string.nav_pond, Icons.Rounded.Water, Icons.Outlined.Water),
    MARKET("market", R.string.nav_market, Icons.Rounded.Storefront, Icons.Outlined.Storefront),
    EVENTS("events", R.string.nav_events, Icons.Rounded.Celebration, Icons.Outlined.Celebration),
    RANKS("ranks", R.string.nav_ranks, Icons.Rounded.Leaderboard, Icons.Outlined.Leaderboard),
    STUDIO("studio", R.string.nav_studio, Icons.Rounded.Face, Icons.Outlined.Face)
}

@Composable
fun DuckLandApp(
    preferencesRepository: ThemePreferencesRepository,
    notificationManager: PondNotificationManager
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser == null) {
                notificationManager.resetSession()
            }
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        key(user.uid) {
            DuckLandAuthenticatedShell(
                currentUserId = user.uid,
                defaultDisplayName = user.displayName ?: "Pond Collector",
                preferencesRepository = preferencesRepository,
                notificationManager = notificationManager
            )
        }
    }
}

@Composable
private fun DuckLandAuthenticatedShell(
    currentUserId: String,
    defaultDisplayName: String,
    preferencesRepository: ThemePreferencesRepository,
    notificationManager: PondNotificationManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val pondViewModel: PondViewModel = viewModel(
        key = "pond_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                PondViewModel(
                    currentUserId = currentUserId,
                    defaultDisplayName = defaultDisplayName,
                    collectorRepository = FirestoreCollectorRepository(db),
                    marketplaceRepository = FirestoreMarketplaceRepository(db),
                    eventRepository = FirestoreEventRepository(db),
                    missionRepository = FirestoreMissionRepository(db),
                    ownerRepository = FirestoreOwnerRepository(db),
                    preferencesRepository = preferencesRepository,
                    notificationManager = notificationManager
                )
            }
        }
    )

    val marketViewModel: MarketplaceViewModel = viewModel(
        key = "market_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                MarketplaceViewModel(
                    currentUserId = currentUserId,
                    collectorRepository = FirestoreCollectorRepository(db),
                    marketplaceRepository = FirestoreMarketplaceRepository(db),
                    missionRepository = FirestoreMissionRepository(db)
                )
            }
        }
    )

    val eventsViewModel: EventsViewModel = viewModel(
        key = "events_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                EventsViewModel(
                    currentUserId = currentUserId,
                    collectorRepository = FirestoreCollectorRepository(db),
                    eventRepository = FirestoreEventRepository(db),
                    missionRepository = FirestoreMissionRepository(db)
                )
            }
        }
    )

    val leaderboardViewModel: LeaderboardViewModel = viewModel(
        key = "leaderboard_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                LeaderboardViewModel(
                    collectorRepository = FirestoreCollectorRepository(db)
                )
            }
        }
    )

    val profileViewModel: ProfileViewModel = viewModel(
        key = "profile_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                ProfileViewModel(
                    currentUserId = currentUserId,
                    collectorRepository = FirestoreCollectorRepository(db),
                    preferencesRepository = preferencesRepository
                )
            }
        }
    )

    val ownerViewModel: OwnerViewModel = viewModel(
        key = "owner_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                OwnerViewModel(
                    currentUserId = currentUserId,
                    ownerRepository = FirestoreOwnerRepository(db),
                    eventRepository = FirestoreEventRepository(db)
                )
            }
        }
    )

    val oracleViewModel: DuckOracleViewModel = viewModel(
        key = "oracle_$currentUserId",
        factory = viewModelFactory {
            initializer {
                DuckOracleViewModel(oracleRepository = FirebaseDuckOracleRepository())
            }
        }
    )

    val pondState by pondViewModel.uiState.collectAsStateWithLifecycle()
    val pondBusy by pondViewModel.isBusy.collectAsStateWithLifecycle()
    val pondMessage by pondViewModel.actionMessage.collectAsStateWithLifecycle()
    val hatchedDuck by pondViewModel.hatchedDuck.collectAsStateWithLifecycle()
    val remoteVersionConfig by pondViewModel.versionConfig.collectAsStateWithLifecycle()
    val mandatoryUpdateConfig by pondViewModel.mandatoryUpdateConfig.collectAsStateWithLifecycle()

    val marketState by marketViewModel.uiState.collectAsStateWithLifecycle()
    val marketBusy by marketViewModel.isBusy.collectAsStateWithLifecycle()
    val marketMessage by marketViewModel.actionMessage.collectAsStateWithLifecycle()

    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()
    val eventsBusy by eventsViewModel.isBusy.collectAsStateWithLifecycle()
    val eventsMessage by eventsViewModel.actionMessage.collectAsStateWithLifecycle()

    val leaderboardState by leaderboardViewModel.uiState.collectAsStateWithLifecycle()
    val leaderboardSort by leaderboardViewModel.selectedSort.collectAsStateWithLifecycle()

    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val profileBusy by profileViewModel.isBusy.collectAsStateWithLifecycle()
    val profileMessage by profileViewModel.actionMessage.collectAsStateWithLifecycle()

    val isOwner by ownerViewModel.isOwner.collectAsStateWithLifecycle()
    val redeemedCodes by ownerViewModel.redeemedCodes.collectAsStateWithLifecycle()
    val allCollectors by ownerViewModel.allCollectors.collectAsStateWithLifecycle()
    val ownerEvents by ownerViewModel.events.collectAsStateWithLifecycle()
    val ownerBusy by ownerViewModel.isBusy.collectAsStateWithLifecycle()
    val ownerMessage by ownerViewModel.actionMessage.collectAsStateWithLifecycle()
    val showChangingToOwner by ownerViewModel.showChangingToOwner.collectAsStateWithLifecycle()
    val showOwnerPanel by ownerViewModel.showOwnerPanel.collectAsStateWithLifecycle()
    val lastPromoReward by ownerViewModel.lastPromoReward.collectAsStateWithLifecycle()

    val oracleChatState by oracleViewModel.chatState.collectAsStateWithLifecycle()
    val oracleLoreState by oracleViewModel.loreState.collectAsStateWithLifecycle()
    val oracleStylistState by oracleViewModel.stylistState.collectAsStateWithLifecycle()
    val showOracleModal by oracleViewModel.showOracleModal.collectAsStateWithLifecycle()
    val oracleInitialTab by oracleViewModel.initialTab.collectAsStateWithLifecycle()
    val oracleLoreDuckId by oracleViewModel.selectedLoreDuckId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var hasNotifPerm by remember { mutableStateOf(notificationManager.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    LaunchedEffect(Unit) {
        if (!notificationManager.hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(pondMessage) {
        pondMessage?.let {
            snackbarHostState.showSnackbar(it)
            pondViewModel.clearMessage()
        }
    }
    LaunchedEffect(marketMessage) {
        marketMessage?.let {
            snackbarHostState.showSnackbar(it)
            marketViewModel.clearMessage()
        }
    }
    LaunchedEffect(eventsMessage) {
        eventsMessage?.let {
            snackbarHostState.showSnackbar(it)
            eventsViewModel.clearMessage()
        }
    }
    LaunchedEffect(profileMessage) {
        profileMessage?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.clearMessage()
        }
    }
    LaunchedEffect(ownerMessage) {
        ownerMessage?.let {
            snackbarHostState.showSnackbar(it)
            ownerViewModel.clearMessage()
        }
    }

    val currentProfile = (pondState as? UiState.Success)?.data?.profile
    if (currentProfile?.banned == true && !isOwner) {
        BannedCollectorScreen(
            reason = currentProfile.banReason,
            onSignOut = {
                notificationManager.resetSession()
                signOut(credentialManager, scope)
            }
        )
        return
    }

    val requiredUpdate = mandatoryUpdateConfig
    if (requiredUpdate != null && !(isOwner && showOwnerPanel)) {
        MandatoryUpdateScreen(
            config = requiredUpdate,
            isOwner = isOwner,
            onOpenStore = {
                val targetUrl = requiredUpdate.storeUrl.ifBlank {
                    "market://details?id=${context.packageName}"
                }
                val fallbackUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                }.onFailure {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                    }
                }
            },
            onOpenOwnerConsole = { ownerViewModel.openOwnerPanel() }
        )
        return
    }

    val notifications = (pondState as? UiState.Success)?.data?.notifications.orEmpty()
    val unreadNotifications = notifications.count { !it.read }
    val pendingTrades = (marketState as? UiState.Success)?.data?.tradeOffers?.count {
        it.recipientId == currentUserId && it.status == "pending"
    } ?: 0

    if (showNotificationsDialog) {
        NotificationsInboxDialog(
            notifications = notifications,
            onMarkRead = { profileViewModel.markNotificationRead(it) },
            onMarkAllRead = { profileViewModel.markAllNotificationsRead() },
            onDismiss = { showNotificationsDialog = false }
        )
    }

    lastPromoReward?.let { reward ->
        PromoRewardDialog(
            reward = reward,
            onDismiss = { ownerViewModel.dismissPromoReward() }
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: DuckLandDestination.POND.route

    GrapeAppBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthClass = WindowWidthClass.fromWidth(maxWidth)
            val useNavRail = widthClass != WindowWidthClass.COMPACT

            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            windowInsets = WindowInsets.navigationBars
                        ) {
                        DuckLandDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest) {
                                DuckLandDestination.MARKET -> pendingTrades
                                DuckLandDestination.STUDIO -> unreadNotifications
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(DuckLandDestination.POND.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text("$badgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DuckLandDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val badgeCount = when (dest) {
                                DuckLandDestination.MARKET -> pendingTrades
                                DuckLandDestination.STUDIO -> unreadNotifications
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(DuckLandDestination.POND.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge { Text("$badgeCount") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag("nav_rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = DuckLandDestination.POND.route
                    ) {
                        composable(DuckLandDestination.POND.route) {
                            PondScreen(
                                uiState = pondState,
                                isBusy = pondBusy || ownerBusy,
                                hatchedDuck = hatchedDuck,
                                isOwner = isOwner,
                                redeemedCodes = redeemedCodes,
                                onHatchEgg = { pondViewModel.hatchEgg(it) },
                                onClaimDailySplash = { pondViewModel.claimDailySplash() },
                                onClaimMission = { pondViewModel.claimDailyMission(it) },
                                onRenameDuck = { id, name -> pondViewModel.renameDuck(id, name) },
                                onListDuck = { duck, coins, trade, rarity ->
                                    pondViewModel.listDuckOnMarketplace(duck, coins, trade, rarity)
                                },
                                onOpenNotifications = { showNotificationsDialog = true },
                                onDismissHatchedDuck = { pondViewModel.dismissHatchedDuck() },
                                onRedeemCode = { code, coins -> ownerViewModel.redeemCode(code, coins) },
                                onOpenOwnerPanel = { ownerViewModel.openOwnerPanel() },
                                onOpenOracle = { tab, duck ->
                                    val activeEvent = ownerEvents.firstOrNull { it.active }?.title
                                    oracleViewModel.openOracle(tab, duck, activeEvent)
                                }
                            )
                        }

                        composable(DuckLandDestination.MARKET.route) {
                            MarketplaceScreen(
                                currentUserId = currentUserId,
                                uiState = marketState,
                                isBusy = marketBusy,
                                onBuyListing = { marketViewModel.buyListing(it) },
                                onCancelListing = { marketViewModel.cancelListing(it) },
                                onProposeTrade = { duck, listing, coins, note ->
                                    marketViewModel.proposeTrade(duck, listing, coins, note)
                                },
                                onAcceptTrade = { marketViewModel.acceptTrade(it) },
                                onDeclineTrade = { marketViewModel.declineTrade(it) },
                                onCancelTrade = { marketViewModel.cancelTrade(it) }
                            )
                        }

                        composable(DuckLandDestination.EVENTS.route) {
                            EventsScreen(
                                currentUserId = currentUserId,
                                uiState = eventsState,
                                isBusy = eventsBusy,
                                onSelectEvent = { eventsViewModel.selectEvent(it) },
                                onSubmitDuck = { duck, caption ->
                                    eventsViewModel.submitDuckEntry(duck, caption)
                                },
                                onCheerEntry = { eventsViewModel.cheerEntry(it) }
                            )
                        }

                        composable(DuckLandDestination.RANKS.route) {
                            LeaderboardScreen(
                                currentUserId = currentUserId,
                                isOwner = isOwner,
                                selectedSort = leaderboardSort,
                                uiState = leaderboardState,
                                onSelectSort = { leaderboardViewModel.selectSort(it) }
                            )
                        }

                        composable(DuckLandDestination.STUDIO.route) {
                            ProfileAvatarScreen(
                                uiState = profileState,
                                isBusy = profileBusy || ownerBusy,
                                hasOsNotificationPermission = hasNotifPerm,
                                isOwner = isOwner,
                                redeemedCodes = redeemedCodes,
                                onSaveProfile = { name, bio, avatar ->
                                    profileViewModel.saveAvatarAndProfile(name, bio, avatar)
                                },
                                onThemeChange = { profileViewModel.setThemePreference(it) },
                                onNotificationsToggle = { profileViewModel.setNotificationsEnabled(it) },
                                onPermissionResult = { hasNotifPerm = it },
                                onRedeemCode = { code, coins -> ownerViewModel.redeemCode(code, coins) },
                                onOpenOwnerPanel = { ownerViewModel.openOwnerPanel() },
                                onOpenAiStylist = {
                                    val activeEvent = ownerEvents.firstOrNull { it.active }?.title
                                    oracleViewModel.openOracle(2, null, activeEvent)
                                },
                                onSignOut = {
                                    notificationManager.resetSession()
                                    signOut(credentialManager, scope)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showOracleModal) {
            val pondData = (pondState as? UiState.Success)?.data
            val activeEventTitle = ownerEvents.firstOrNull { it.active }?.title
            val myDucks = pondData?.ducks.orEmpty()
            val myCoins = pondData?.wallet?.coins ?: 0L
            DuckOracleSheet(
                initialTab = oracleInitialTab,
                myDucks = myDucks,
                coins = myCoins,
                activeEventTitle = activeEventTitle,
                selectedLoreDuckId = oracleLoreDuckId,
                chatState = oracleChatState,
                loreState = oracleLoreState,
                stylistState = oracleStylistState,
                onAskOracle = { prompt ->
                    oracleViewModel.askOracle(prompt, myDucks, myCoins, activeEventTitle)
                },
                onGenerateLore = { duck ->
                    oracleViewModel.generateLoreForDuck(duck, activeEventTitle)
                },
                onSuggestAvatar = { vibe ->
                    oracleViewModel.suggestAvatarLook(vibe)
                },
                onApplyAvatarSuggestion = { suggestedPalette ->
                    val prof = pondData?.profile
                    profileViewModel.saveAvatarAndProfile(
                        displayName = prof?.displayName?.ifBlank { defaultDisplayName } ?: defaultDisplayName,
                        bio = prof?.bio ?: "",
                        avatar = suggestedPalette
                    )
                    oracleViewModel.closeOracle()
                },
                onClose = { oracleViewModel.closeOracle() }
            )
        }

        if (showOwnerPanel && isOwner) {
            OwnerPanelScreen(
                currentUserId = currentUserId,
                collectors = allCollectors,
                events = ownerEvents,
                versionConfig = remoteVersionConfig,
                isBusy = ownerBusy,
                onGrantDuck = { targetUid, species, name, rarity, palette ->
                    ownerViewModel.grantDuck(targetUid, species, name, rarity, palette)
                },
                onGrantCoins = { targetUid, amount, isAdd ->
                    ownerViewModel.grantCoins(targetUid, amount, isAdd)
                },
                onSendRoyalGift = { targetUid ->
                    ownerViewModel.sendRoyalGift(targetUid)
                },
                onSetBanStatus = { targetUid, banned, reason ->
                    ownerViewModel.setBanStatus(targetUid, banned, reason)
                },
                onSendAnnouncement = { targetUids, title, body ->
                    ownerViewModel.sendAnnouncement(targetUids, title, body)
                },
                onActivateEvent = { eventId ->
                    ownerViewModel.activateEvent(eventId)
                },
                onCreateEvent = { title, tag, desc, bonus, week, activeNow ->
                    ownerViewModel.createEvent(title, tag, desc, bonus, week, activeNow)
                },
                onUpdateVersionConfig = { minCode, latestCode, latestName, msg, url ->
                    ownerViewModel.updateVersionConfig(minCode, latestCode, latestName, msg, url)
                },
                onClose = { ownerViewModel.closeOwnerPanel() }
            )
        }

        if (showChangingToOwner) {
            ChangingToOwnerScreen(
                onFinished = { ownerViewModel.finishChangingToOwnerTransition() }
            )
        }
        }
    }
}

@Composable
private fun MandatoryUpdateScreen(
    config: AppVersionConfig,
    isOwner: Boolean,
    onOpenStore: () -> Unit,
    onOpenOwnerConsole: () -> Unit
) {
    GrapeAppBackground {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("mandatory_update_screen")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.widthIn(max = 440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Pond Update Required",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Installed: v${BuildConfig.VERSION_NAME} (Code ${BuildConfig.VERSION_CODE}) • Required: v${config.latestVersionName} (Code ${config.minRequiredVersionCode}+)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = config.updateMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenStore,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("mandatory_update_store_button")
                        ) {
                            Text("Update Duck Land")
                        }
                        if (isOwner) {
                            OutlinedButton(
                                onClick = onOpenOwnerConsole,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("mandatory_update_owner_override")
                            ) {
                                Text("Owner Version Override")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BannedCollectorScreen(
    reason: String,
    onSignOut: () -> Unit
) {
    GrapeAppBackground {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("banned_collector_screen")
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Pond Access Suspended",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reason.ifBlank { "Your account has been suspended by the Duck Land Owner." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("banned_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun NotificationsInboxDialog(
    notifications: List<PondNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pond Alerts")
                if (notifications.any { !it.read }) {
                    TextButton(onClick = onMarkAllRead) {
                        Text("Mark all read")
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    text = "No trade or event alerts yet. You'll be notified right away when activity splashes in!",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            onClick = { if (!item.read) onMarkRead(item.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
