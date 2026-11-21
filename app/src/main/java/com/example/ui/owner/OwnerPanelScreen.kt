package com.example.ui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CollectorProfile
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.DuckSpecies
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.DuckTraitCatalog
import com.example.domain.model.WeeklyEvent
import com.example.ui.components.DuckCanvas
import com.example.ui.components.GrapeAppBackground
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.RarityBadge
import com.example.ui.components.WindowWidthClass

@Composable
fun OwnerPanelScreen(
    currentUserId: String,
    collectors: List<CollectorProfile>,
    events: List<WeeklyEvent>,
    isBusy: Boolean,
    onGrantDuck: (String, DuckSpecies, String, DuckRarity, DuckPalette) -> Unit,
    onGrantCoins: (String, Long, Boolean) -> Unit,
    onSendRoyalGift: (String) -> Unit,
    onSetBanStatus: (String, Boolean, String) -> Unit,
    onSendAnnouncement: (List<String>, String, String) -> Unit,
    onActivateEvent: (String) -> Unit,
    onCreateEvent: (String, String, String, String, Long, Boolean) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var selectedTargetUid by remember(collectors, currentUserId) {
        mutableStateOf(
            collectors.firstOrNull { it.userId == currentUserId }?.userId
                ?: collectors.firstOrNull()?.userId
                ?: currentUserId
        )
    }
    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("Duck Spawner", "Grape Mint", "Moderation & Alerts", "Weekly Events")

    val selectedTarget = collectors.firstOrNull { it.userId == selectedTargetUid }

    GrapeAppBackground {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("owner_panel_screen")
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = WindowWidthClass.fromWidth(maxWidth) == WindowWidthClass.EXPANDED

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = if (isWide) 1040.dp else 640.dp)
                    .align(Alignment.TopCenter)
            ) {
                item {
                    OwnerHeaderCard(
                        targetName = selectedTarget?.displayName ?: "My Account",
                        isBusy = isBusy,
                        onSendRoyalGift = { onSendRoyalGift(selectedTargetUid) },
                        onClose = onClose
                    )
                }

                item {
                    TargetCollectorSelector(
                        currentUserId = currentUserId,
                        collectors = collectors,
                        selectedUserId = selectedTargetUid,
                        onSelect = { selectedTargetUid = it }
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sections.indices.toList()) { idx ->
                            FilterChip(
                                selected = selectedSection == idx,
                                onClick = { selectedSection = idx },
                                label = { Text(sections[idx]) },
                                modifier = Modifier.testTag("owner_tab_$idx")
                            )
                        }
                    }
                }

                when (selectedSection) {
                    0 -> item {
                        OwnerDuckSpawnerCard(
                            targetName = selectedTarget?.displayName ?: "Target Collector",
                            isBusy = isBusy,
                            onSpawnDuck = { species, customName, rarity, palette ->
                                onGrantDuck(selectedTargetUid, species, customName, rarity, palette)
                            }
                        )
                    }

                    1 -> item {
                        OwnerCoinMintCard(
                            targetName = selectedTarget?.displayName ?: "Target Collector",
                            isBusy = isBusy,
                            onGrantCoins = { amount, isAdd ->
                                onGrantCoins(selectedTargetUid, amount, isAdd)
                            }
                        )
                    }

                    2 -> item {
                        OwnerModerationAndAlertsCard(
                            currentUserId = currentUserId,
                            selectedTarget = selectedTarget,
                            selectedTargetUid = selectedTargetUid,
                            allCollectors = collectors,
                            isBusy = isBusy,
                            onSetBanStatus = onSetBanStatus,
                            onSendAnnouncement = onSendAnnouncement
                        )
                    }

                    3 -> item {
                        OwnerEventsConsoleCard(
                            events = events,
                            isBusy = isBusy,
                            onActivateEvent = onActivateEvent,
                            onCreateEvent = onCreateEvent
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun OwnerHeaderCard(
    targetName: String,
    isBusy: Boolean,
    onSendRoyalGift: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF182A3A),
                            Color(0xFF33260F)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                text = "Duck Land Owner Panel",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFFFFE082),
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Active Target: $targetName",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD9EAE8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_owner_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Owner Panel",
                            tint = Color.White
                        )
                    }
                }

                Button(
                    onClick = onSendRoyalGift,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF1F1602)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("owner_royal_gift_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Royal Gift (+50K Grapes & King Ducky)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TargetCollectorSelector(
    currentUserId: String,
    collectors: List<CollectorProfile>,
    selectedUserId: String,
    onSelect: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Select Target Collector (${collectors.size})", style = MaterialTheme.typography.titleSmall)
            }
            if (collectors.isEmpty()) {
                Text(
                    text = "Targeting your own collector account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(collectors, key = { it.userId }) { collector ->
                        val isMe = collector.userId == currentUserId
                        val label = buildString {
                            append(collector.displayName.ifBlank { "Collector" })
                            if (isMe) append(" (You)")
                            if (collector.banned) append(" [BANNED]")
                        }
                        FilterChip(
                            selected = selectedUserId == collector.userId,
                            onClick = { onSelect(collector.userId) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("target_collector_${collector.userId}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerDuckSpawnerCard(
    targetName: String,
    isBusy: Boolean,
    onSpawnDuck: (DuckSpecies, String, DuckRarity, DuckPalette) -> Unit
) {
    var selectedSpecies by remember { mutableStateOf(DuckSpeciesCatalog.byId("golden_monarch")) }
    var customName by remember(selectedSpecies) { mutableStateOf(selectedSpecies.defaultName) }
    var rarityOverride by remember(selectedSpecies) { mutableStateOf(selectedSpecies.rarity) }
    var palette by remember(selectedSpecies) { mutableStateOf(selectedSpecies.defaultPalette) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Spawn Any Duck for $targetName", style = MaterialTheme.typography.titleLarge)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DuckCanvas(
                    palette = palette,
                    size = 96.dp,
                    cornerRadius = 22.dp
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RarityBadge(rarity = rarityOverride)
                    Text(
                        text = customName.ifBlank { selectedSpecies.defaultName },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedSpecies.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text("1. Choose Species", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DuckSpeciesCatalog.all, key = { it.speciesId }) { species ->
                    FilterChip(
                        selected = selectedSpecies.speciesId == species.speciesId,
                        onClick = {
                            selectedSpecies = species
                            customName = species.defaultName
                            rarityOverride = species.rarity
                            palette = species.defaultPalette
                        },
                        label = { Text(species.defaultName) }
                    )
                }
            }

            Text("2. Rarity Override", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DuckRarity.entries) { r ->
                    FilterChip(
                        selected = rarityOverride == r,
                        onClick = { rarityOverride = r },
                        label = { Text(r.label) }
                    )
                }
            }

            Text("3. Quick Hat & Eyewear Style", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DuckTraitCatalog.hats, key = { it.id }) { hat ->
                    FilterChip(
                        selected = palette.hat == hat.id,
                        onClick = { palette = palette.copy(hat = hat.id) },
                        label = { Text(hat.label) }
                    )
                }
            }

            OutlinedTextField(
                value = customName,
                onValueChange = { if (it.length <= 40) customName = it },
                label = { Text("Custom Duck Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_duck_name_input")
            )

            Button(
                onClick = { onSpawnDuck(selectedSpecies, customName, rarityOverride, palette) },
                enabled = !isBusy,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("owner_spawn_duck_button")
            ) {
                Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Duck to $targetName")
            }
        }
    }
}

@Composable
private fun OwnerCoinMintCard(
    targetName: String,
    isBusy: Boolean,
    onGrantCoins: (Long, Boolean) -> Unit
) {
    var coinAmountText by remember { mutableStateOf("5000") }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GrapeClusterIcon(
                    modifier = Modifier.size(24.dp),
                    grapeColor = MaterialTheme.colorScheme.primary,
                    highlightColor = MaterialTheme.colorScheme.primaryContainer,
                    leafColor = MaterialTheme.colorScheme.secondary,
                    contentDesc = null
                )
                Text("Pond Grape Mint • $targetName", style = MaterialTheme.typography.titleLarge)
            }

            Text("Quick Grape Presets", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(500L, 1_000L, 10_000L, 100_000L)) { amount ->
                    OutlinedButton(
                        onClick = { onGrantCoins(amount, true) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("+$amount")
                    }
                }
            }

            OutlinedTextField(
                value = coinAmountText,
                onValueChange = { if (it.all(Char::isDigit) && it.length <= 9) coinAmountText = it },
                label = { Text("Custom Grape Amount (0 - 100,000,000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_coins_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val amt = coinAmountText.toLongOrNull() ?: 1000L
                        onGrantCoins(amt, true)
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("owner_add_coins_button")
                ) {
                    Text("Add Grapes")
                }
                OutlinedButton(
                    onClick = {
                        val amt = coinAmountText.toLongOrNull() ?: 0L
                        onGrantCoins(amt, false)
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("owner_set_coins_button")
                ) {
                    Text("Set Balance")
                }
            }
        }
    }
}

@Composable
private fun OwnerModerationAndAlertsCard(
    currentUserId: String,
    selectedTarget: CollectorProfile?,
    selectedTargetUid: String,
    allCollectors: List<CollectorProfile>,
    isBusy: Boolean,
    onSetBanStatus: (String, Boolean, String) -> Unit,
    onSendAnnouncement: (List<String>, String, String) -> Unit
) {
    var banReason by remember(selectedTargetUid) {
        mutableStateOf(selectedTarget?.banReason?.ifBlank { "Violated Duck Land Pond rules" } ?: "Violated Duck Land Pond rules")
    }
    var announceTitle by remember { mutableStateOf("Royal Pond Drop!") }
    var announceBody by remember { mutableStateOf("Check your pond! The Owner just dropped rare surprises across Duck Land.") }
    var broadcastAll by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Account Moderation", style = MaterialTheme.typography.titleLarge)
                }

                val isSelf = selectedTargetUid == currentUserId
                val isCurrentlyBanned = selectedTarget?.banned == true

                Text(
                    text = when {
                        isSelf -> "You are currently targeting your own Owner account. Select another collector above to ban or unban."
                        isCurrentlyBanned -> "${selectedTarget?.displayName ?: "Collector"} is currently BANNED (${selectedTarget?.banReason})."
                        else -> "${selectedTarget?.displayName ?: "Collector"} is in good standing."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentlyBanned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isSelf) {
                    OutlinedTextField(
                        value = banReason,
                        onValueChange = { if (it.length <= 200) banReason = it },
                        label = { Text("Suspension / Ban Reason") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_ban_reason_input")
                    )

                    if (isCurrentlyBanned) {
                        Button(
                            onClick = { onSetBanStatus(selectedTargetUid, false, "") },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("owner_unban_button")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unban Collector")
                        }
                    } else {
                        Button(
                            onClick = { onSetBanStatus(selectedTargetUid, true, banReason) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("owner_ban_button")
                        ) {
                            Icon(Icons.Rounded.Block, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ban Collector from Duck Land")
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Push Pond Announcement", style = MaterialTheme.typography.titleLarge)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Broadcast to all ${allCollectors.size} collectors", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = broadcastAll, onCheckedChange = { broadcastAll = it })
                }

                OutlinedTextField(
                    value = announceTitle,
                    onValueChange = { if (it.length <= 80) announceTitle = it },
                    label = { Text("Notification Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = announceBody,
                    onValueChange = { if (it.length <= 200) announceBody = it },
                    label = { Text("Announcement Message") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val targets = if (broadcastAll) {
                            allCollectors.map { it.userId }.ifEmpty { listOf(selectedTargetUid) }
                        } else {
                            listOf(selectedTargetUid)
                        }
                        onSendAnnouncement(targets, announceTitle, announceBody)
                    },
                    enabled = !isBusy && announceTitle.isNotBlank() && announceBody.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("owner_send_announcement_button")
                ) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (broadcastAll) "Send to Everyone" else "Send to ${selectedTarget?.displayName ?: "Target"}")
                }
            }
        }
    }
}

@Composable
private fun OwnerEventsConsoleCard(
    events: List<WeeklyEvent>,
    isBusy: Boolean,
    onActivateEvent: (String) -> Unit,
    onCreateEvent: (String, String, String, String, Long, Boolean) -> Unit
) {
    val nextWeek = (events.maxOfOrNull { it.weekNumber } ?: 49L) + 1L
    var title by remember { mutableStateOf("") }
    var themeTag by remember(nextWeek) { mutableStateOf("Week $nextWeek") }
    var description by remember { mutableStateOf("") }
    var bonusRarity by remember { mutableStateOf("epic") }
    var activateNow by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Switch Active Spotlight Event", style = MaterialTheme.typography.titleLarge)
                }

                events.forEach { ev ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (ev.active) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isBusy && !ev.active) { onActivateEvent(ev.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${ev.themeTag} • Bonus: ${ev.bonusRarity.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (ev.active) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                OutlinedButton(
                                    onClick = { onActivateEvent(ev.id) },
                                    enabled = !isBusy
                                ) {
                                    Text("Activate")
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Launch New Weekly Event", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("Event Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = themeTag,
                    onValueChange = { if (it.length <= 30) themeTag = it },
                    label = { Text("Theme Tag (e.g. Week $nextWeek)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("Event Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Bonus Rarity", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("any", "common", "rare", "epic", "legendary")) { opt ->
                        FilterChip(
                            selected = bonusRarity == opt,
                            onClick = { bonusRarity = opt },
                            label = { Text(opt.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as live spotlight immediately", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = activateNow, onCheckedChange = { activateNow = it })
                }
                Button(
                    onClick = {
                        onCreateEvent(title, themeTag, description, bonusRarity, nextWeek, activateNow)
                        title = ""
                        description = ""
                    },
                    enabled = !isBusy && title.isNotBlank() && description.isNotBlank(),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("owner_create_event_button")
                ) {
                    Text("Publish Weekly Event")
                }
            }
        }
    }
}
