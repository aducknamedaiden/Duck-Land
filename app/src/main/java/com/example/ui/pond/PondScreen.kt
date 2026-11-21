package com.example.ui.pond

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DailyMissionProgress
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.RubberDuck
import com.example.domain.model.UiState
import com.example.ui.components.DuckCanvas
import com.example.ui.components.EmptyPondCard
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.NotificationBellChip
import com.example.ui.components.PondCoinsChip
import com.example.ui.components.RarityBadge
import com.example.ui.components.RarityDuckCardContainer
import com.example.ui.components.WindowWidthClass
import com.example.ui.owner.PromoCodesBannerCard

@Composable
fun PondScreen(
    uiState: UiState<PondDashboardData>,
    isBusy: Boolean,
    hatchedDuck: RubberDuck?,
    isOwner: Boolean,
    redeemedCodes: Set<String>,
    onHatchEgg: (Boolean) -> Unit,
    onClaimDailySplash: () -> Unit,
    onClaimMission: (DailyMissionProgress) -> Unit,
    onRenameDuck: (String, String) -> Unit,
    onListDuck: (RubberDuck, Long, Boolean, String) -> Unit,
    onOpenNotifications: () -> Unit,
    onDismissHatchedDuck: () -> Unit,
    onRedeemCode: (String, Long) -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onOpenOracle: (Int, RubberDuck?) -> Unit
) {
    var selectedRarityFilter by remember { mutableStateOf<DuckRarity?>(null) }
    var inspectedDuck by remember { mutableStateOf<RubberDuck?>(null) }
    var duckToList by remember { mutableStateOf<RubberDuck?>(null) }

    if (hatchedDuck != null) {
        HatchedDuckRevealDialog(
            duck = hatchedDuck,
            onDismiss = onDismissHatchedDuck
        )
    }

    if (duckToList != null) {
        ListDuckDialog(
            duck = duckToList!!,
            onDismiss = { duckToList = null },
            onConfirm = { coins, wantsTrade, desiredRarity ->
                onListDuck(duckToList!!, coins, wantsTrade, desiredRarity)
                duckToList = null
                inspectedDuck = null
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        val isSplitPane = widthClass == WindowWidthClass.EXPANDED

        if (inspectedDuck != null && !isSplitPane) {
            val allDucks = (uiState as? UiState.Success)?.data?.ducks.orEmpty()
            val liveInspected = allDucks.firstOrNull { it.id == inspectedDuck?.id } ?: inspectedDuck!!
            val sameSpeciesDucks = allDucks.filter { it.speciesId == liveInspected.speciesId }
                .ifEmpty { listOf(liveInspected) }
            BackHandler { inspectedDuck = null }
            DuckDetailDialog(
                duck = liveInspected,
                groupDucks = sameSpeciesDucks,
                onSelectDuck = { inspectedDuck = it },
                onDismiss = { inspectedDuck = null },
                onRename = { newName ->
                    onRenameDuck(liveInspected.id, newName)
                    inspectedDuck = null
                },
                onListClick = { duckToList = liveInspected },
                onAskOracleLore = {
                    inspectedDuck = null
                    onOpenOracle(1, liveInspected)
                }
            )
        }

        when (uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPondCard(
                        title = "Pond connection hiccup",
                        subtitle = uiState.message
                    )
                }
            }

            is UiState.Success -> {
                val data = uiState.data
                val filteredDucks = remember(data.ducks, selectedRarityFilter) {
                    if (selectedRarityFilter == null) data.ducks
                    else data.ducks.filter { DuckRarity.fromId(it.rarity) == selectedRarityFilter }
                }
                val groupedDucks = remember(filteredDucks) {
                    filteredDucks
                        .groupBy { it.speciesId }
                        .entries
                        .map { (speciesId, copies) ->
                            val representative = copies.firstOrNull { !it.listed } ?: copies.first()
                            DuckSpeciesGroup(
                                speciesId = speciesId,
                                ducks = copies,
                                primaryDuck = representative
                            )
                        }
                }
                val activeInspected = inspectedDuck?.let { current ->
                    data.ducks.firstOrNull { it.id == current.id }
                }
                val activeGroupDucks = remember(data.ducks, activeInspected) {
                    if (activeInspected == null) emptyList()
                    else data.ducks.filter { it.speciesId == activeInspected.speciesId }
                        .ifEmpty { listOf(activeInspected) }
                }
                val unreadCount = data.notifications.count { !it.read }
                val columns = when (widthClass) {
                    WindowWidthClass.COMPACT -> 2
                    WindowWidthClass.MEDIUM -> 3
                    WindowWidthClass.EXPANDED -> if (activeInspected != null) 2 else 3
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PondTopHeader(
                                displayName = data.profile?.displayName ?: "Collector",
                                avatar = DuckPalette.fromMap(data.profile?.avatarConfig),
                                coins = data.wallet?.coins ?: 0L,
                                unreadCount = unreadCount,
                                isOwner = isOwner,
                                onOpenNotifications = onOpenNotifications,
                                onOpenOwnerPanel = onOpenOwnerPanel
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            HatcheryBannerCard(
                                isBusy = isBusy,
                                onHatchStandard = { onHatchEgg(false) },
                                onHatchLucky = { onHatchEgg(true) },
                                onClaimDaily = onClaimDailySplash
                            )
                        }

                        if (data.dailyMissions.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                DailyMissionsCard(
                                    missions = data.dailyMissions,
                                    isBusy = isBusy,
                                    onClaimMission = onClaimMission
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PondOracleLauncherCard(
                                onOpenChat = { onOpenOracle(0, null) },
                                onOpenStylist = { onOpenOracle(2, null) }
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PromoCodesBannerCard(
                                redeemedCodes = redeemedCodes,
                                isOwner = isOwner,
                                isBusy = isBusy,
                                onRedeemCode = { code ->
                                    onRedeemCode(code, data.wallet?.coins ?: 0L)
                                },
                                onOpenOwnerPanel = onOpenOwnerPanel
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (groupedDucks.size == filteredDucks.size) {
                                            "My Flock (${filteredDucks.size})"
                                        } else {
                                            "My Flock (${filteredDucks.size} • ${groupedDucks.size} species)"
                                        },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FilterChip(
                                            selected = selectedRarityFilter == null,
                                            onClick = { selectedRarityFilter = null },
                                            label = { Text("All") },
                                            modifier = Modifier.testTag("filter_rarity_all")
                                        )
                                    }
                                    items(DuckRarity.entries) { rarity ->
                                        FilterChip(
                                            selected = selectedRarityFilter == rarity,
                                            onClick = {
                                                selectedRarityFilter =
                                                    if (selectedRarityFilter == rarity) null else rarity
                                            },
                                            label = { Text(rarity.label) },
                                            modifier = Modifier.testTag("filter_rarity_${rarity.id}")
                                        )
                                    }
                                }
                            }
                        }

                        if (groupedDucks.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPondCard(
                                    title = stringResource(R.string.empty_pond_title),
                                    subtitle = stringResource(R.string.empty_pond_subtitle)
                                )
                            }
                        } else {
                            items(groupedDucks, key = { it.speciesId }) { group ->
                                val isGroupSelected = activeInspected != null &&
                                    group.ducks.any { it.id == activeInspected.id }
                                val displayDuck = if (isGroupSelected) activeInspected!! else group.primaryDuck
                                DuckCollectionCard(
                                    duck = displayDuck,
                                    duplicateCount = group.ducks.size,
                                    listedCount = group.ducks.count { it.listed },
                                    selected = isGroupSelected,
                                    onClick = { inspectedDuck = group.primaryDuck }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSplitPane && activeInspected != null) {
                        if (activeInspected != null) {
                            Surface(
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                                modifier = Modifier
                                    .width(340.dp)
                                    .fillMaxHeight()
                            ) {
                                DuckDetailPane(
                                    duck = activeInspected,
                                    groupDucks = activeGroupDucks,
                                    onSelectDuck = { inspectedDuck = it },
                                    onClose = { inspectedDuck = null },
                                    onRename = { newName -> onRenameDuck(activeInspected.id, newName) },
                                    onListClick = { duckToList = activeInspected },
                                    onAskOracleLore = { onOpenOracle(1, activeInspected) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PondTopHeader(
    displayName: String,
    avatar: DuckPalette,
    coins: Long,
    unreadCount: Int,
    isOwner: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenOwnerPanel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            DuckCanvas(
                palette = avatar,
                size = 52.dp,
                cornerRadius = 18.dp
            )
            Column {
                Text(
                    text = "Duck Land :)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isOwner) "Owner • $displayName" else "Hi, $displayName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isOwner) {
                Surface(
                    onClick = onOpenOwnerPanel,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.testTag("header_owner_chip")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = "Open Owner Panel",
                        tint = Color(0xFF1F1602),
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
            }
            PondCoinsChip(coins = coins)
            NotificationBellChip(unreadCount = unreadCount, onClick = onOpenNotifications)
        }
    }
}

@Composable
private fun HatcheryBannerCard(
    isBusy: Boolean,
    onHatchStandard: () -> Unit,
    onHatchLucky: () -> Unit,
    onClaimDaily: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pond Hatchery",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hatch mystery ducklings or claim your daily splash grapes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onClaimDaily,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("claim_daily_bonus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+75 Daily")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onHatchStandard,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hatch_standard_egg_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Egg • 60")
                    }
                    Button(
                        onClick = onHatchLucky,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hatch_lucky_egg_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lucky • 140")
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMissionsCard(
    missions: List<DailyMissionProgress>,
    isBusy: Boolean,
    onClaimMission: (DailyMissionProgress) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val claimableCount = missions.count { it.isCompleted && !it.claimed }
    val completedCount = missions.count { it.isCompleted }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_missions_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .testTag("daily_missions_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Daily Pond Missions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (claimableCount > 0) "$claimableCount Grape reward ready to claim!"
                            else "$completedCount/${missions.size} completed today",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (claimableCount > 0) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse daily missions" else "Expand daily missions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    missions.forEach { mission ->
                        DailyMissionRow(
                            mission = mission,
                            isBusy = isBusy,
                            onClaim = { onClaimMission(mission) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyMissionRow(
    mission: DailyMissionProgress,
    isBusy: Boolean,
    onClaim: () -> Unit
) {
    val fraction = if (mission.target > 0L) {
        (mission.progress.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (mission.isCompleted && !mission.claimed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mission_row_${mission.missionId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = mission.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "${mission.progress}/${mission.target}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = mission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = if (mission.isCompleted) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            when {
                mission.claimed -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Mission claimed",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Claimed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                mission.isCompleted -> {
                    Button(
                        onClick = onClaim,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("claim_mission_${mission.missionId}")
                    ) {
                        GrapeClusterIcon(
                            modifier = Modifier.size(15.dp),
                            grapeColor = MaterialTheme.colorScheme.onPrimary,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer,
                            leafColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentDesc = null
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("+${mission.rewardCoins}")
                    }
                }

                else -> {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GrapeClusterIcon(
                                modifier = Modifier.size(14.dp),
                                grapeColor = MaterialTheme.colorScheme.primary,
                                highlightColor = MaterialTheme.colorScheme.primaryContainer,
                                leafColor = MaterialTheme.colorScheme.secondary,
                                contentDesc = null
                            )
                            Text(
                                text = "+${mission.rewardCoins}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DuckSpeciesGroup(
    val speciesId: String,
    val ducks: List<RubberDuck>,
    val primaryDuck: RubberDuck
)

@Composable
private fun DuckCollectionCard(
    duck: RubberDuck,
    duplicateCount: Int,
    listedCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val rarity = DuckRarity.fromId(duck.rarity)
    RarityDuckCardContainer(
        rarity = rarity,
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duck_card_${duck.speciesId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DuckCanvas(
                    palette = DuckPalette.fromMap(duck.palette),
                    size = 112.dp,
                    cornerRadius = 18.dp
                )
                if (duplicateCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .testTag("dupe_badge_${duck.speciesId}")
                    ) {
                        Text(
                            text = "[$duplicateCount]",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (listedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (listedCount > 1) "$listedCount Listed" else "Listed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = duck.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RarityBadge(rarity = rarity)
                Text(
                    text = if (duplicateCount > 1) "#${duck.serialNumber} • [$duplicateCount]" else "#${duck.serialNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DuckDetailDialog(
    duck: RubberDuck,
    groupDucks: List<RubberDuck>,
    onSelectDuck: (RubberDuck) -> Unit,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onListClick: () -> Unit,
    onAskOracleLore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            DuckDetailPane(
                duck = duck,
                groupDucks = groupDucks,
                onSelectDuck = onSelectDuck,
                onClose = onDismiss,
                onRename = onRename,
                onListClick = onListClick,
                onAskOracleLore = onAskOracleLore
            )
        }
    )
}

@Composable
private fun DuckDetailPane(
    duck: RubberDuck,
    groupDucks: List<RubberDuck>,
    onSelectDuck: (RubberDuck) -> Unit,
    onClose: () -> Unit,
    onRename: (String) -> Unit,
    onListClick: () -> Unit,
    onAskOracleLore: () -> Unit
) {
    val rarity = DuckRarity.fromId(duck.rarity)
    val species = DuckSpeciesCatalog.byId(duck.speciesId)
    var editingName by remember(duck.id) { mutableStateOf(false) }
    var draftName by remember(duck.id, duck.name) { mutableStateOf(duck.name) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RarityBadge(rarity = rarity)
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close duck details")
            }
        }

        DuckCanvas(
            palette = DuckPalette.fromMap(duck.palette),
            size = 160.dp,
            cornerRadius = 28.dp
        )

        if (groupDucks.size > 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Duplicate Copies [${groupDucks.size}]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groupDucks, key = { it.id }) { copy ->
                        FilterChip(
                            selected = copy.id == duck.id,
                            onClick = { onSelectDuck(copy) },
                            label = {
                                Text(
                                    text = if (copy.listed) "#${copy.serialNumber} (Listed)" else "#${copy.serialNumber}"
                                )
                            }
                        )
                    }
                }
            }
        }

        if (editingName) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = { if (it.length <= 40) draftName = it },
                    singleLine = true,
                    label = { Text("Nickname") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (draftName.isNotBlank()) {
                            onRename(draftName)
                            editingName = false
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Save")
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = duck.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = { editingName = true }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Rename duck",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Text(
            text = "${species.defaultName} • Serial #${duck.serialNumber}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = species.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!duck.listed) {
            Button(
                onClick = onListClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("list_duck_button")
            ) {
                Icon(Icons.Rounded.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("List on Marketplace")
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Currently listed on the Marketplace. Manage or cancel it in the Market tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedButton(
            onClick = onAskOracleLore,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("duck_detail_lore_button")
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask Gemini for Duck Lore")
        }
    }
}

@Composable
private fun PondOracleLauncherCard(
    onOpenChat: () -> Unit,
    onOpenStylist: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text("Pond AI Oracle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Gemini trade tips, lore & styling",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onOpenStylist,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Style")
                }
                Button(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_pond_oracle_button")
                ) {
                    Text("Ask AI")
                }
            }
        }
    }
}

@Composable
private fun ListDuckDialog(
    duck: RubberDuck,
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean, String) -> Unit
) {
    val rarity = DuckRarity.fromId(duck.rarity)
    var priceText by remember { mutableStateOf(rarity.suggestedPrice.toString()) }
    var wantsTrade by remember { mutableStateOf(true) }
    var desiredRarity by remember { mutableStateOf("any") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("List ${duck.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.all(Char::isDigit)) priceText = it },
                    label = { Text("Asking Grapes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listing_price_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accept duck-for-duck trades", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = wantsTrade, onCheckedChange = { wantsTrade = it })
                }
                if (wantsTrade) {
                    Text("Desired trade rarity", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("any", "common", "rare", "epic", "legendary")) { option ->
                            FilterChip(
                                selected = desiredRarity == option,
                                onClick = { desiredRarity = option },
                                label = { Text(option.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val coins = priceText.toLongOrNull()?.coerceIn(1L, 1_000_000L) ?: rarity.suggestedPrice
                    onConfirm(coins, wantsTrade, desiredRarity)
                },
                modifier = Modifier.testTag("confirm_create_listing_button")
            ) {
                Text("Post Listing")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HatchedDuckRevealDialog(
    duck: RubberDuck,
    onDismiss: () -> Unit
) {
    val rarity = DuckRarity.fromId(duck.rarity)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("A New Duck Hatched!") },
        text = {
            RarityDuckCardContainer(
                rarity = rarity,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DuckCanvas(
                        palette = DuckPalette.fromMap(duck.palette),
                        size = 148.dp,
                        cornerRadius = 26.dp
                    )
                    Text(
                        text = duck.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RarityBadge(rarity = rarity)
                    Text(
                        text = "Serial #${duck.serialNumber} joined your pond!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_hatch_reveal_button")
            ) {
                Text("Welcome to the Pond!")
            }
        }
    )
}
