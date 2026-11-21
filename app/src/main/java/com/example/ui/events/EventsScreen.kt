package com.example.ui.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.EventEntry
import com.example.domain.model.RubberDuck
import com.example.domain.model.UiState
import com.example.domain.model.WeeklyEvent
import com.example.ui.components.DuckCanvas
import com.example.ui.components.EmptyPondCard
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.PondCoinsChip
import com.example.ui.components.RarityBadge
import com.example.ui.components.RarityDuckCardContainer
import com.example.ui.components.WindowWidthClass

private enum class ShowcaseSortOrder(val label: String) {
    TOP_VOTED("Top Ranked"),
    NEWEST("Newest")
}

@Composable
fun EventsScreen(
    currentUserId: String,
    uiState: UiState<EventsDashboardData>,
    isBusy: Boolean,
    onSelectEvent: (String) -> Unit,
    onSubmitDuck: (RubberDuck, String) -> Unit,
    onCheerEntry: (EventEntry) -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(ShowcaseSortOrder.TOP_VOTED) }

    if (showSubmitDialog && uiState is UiState.Success) {
        SubmitEventDuckDialog(
            eventTitle = uiState.data.selectedEvent?.title ?: "Weekly Showcase",
            myDucks = uiState.data.myDucks,
            onDismiss = { showSubmitDialog = false },
            onSubmit = { duck, caption ->
                onSubmitDuck(duck, caption)
                showSubmitDialog = false
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
                    title = "Showcase unavailable",
                    subtitle = uiState.message
                )
            }
        }

        is UiState.Success -> {
            val data = uiState.data
            val rankMap = remember(data.entries) {
                data.entries.mapIndexed { index, entry -> entry.id to (index + 1) }.toMap()
            }
            val displayedEntries = remember(data.entries, sortOrder) {
                when (sortOrder) {
                    ShowcaseSortOrder.TOP_VOTED -> data.entries
                    ShowcaseSortOrder.NEWEST -> data.entries.sortedByDescending {
                        it.createdAt?.seconds ?: 0L
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthClass = WindowWidthClass.fromWidth(maxWidth)
                val columns = when (widthClass) {
                    WindowWidthClass.COMPACT -> 1
                    WindowWidthClass.MEDIUM -> 2
                    WindowWidthClass.EXPANDED -> 3
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Weekly Pond Showcase",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                PondCoinsChip(coins = data.wallet?.coins ?: 0L)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(data.events, key = { it.id }) { event ->
                                    FilterChip(
                                        selected = data.selectedEvent?.id == event.id,
                                        onClick = { onSelectEvent(event.id) },
                                        label = { Text(event.themeTag) },
                                        modifier = Modifier.testTag("event_chip_${event.id}")
                                    )
                                }
                            }
                        }
                    }

                    data.selectedEvent?.let { activeEvent ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EventHeroBanner(
                                event = activeEvent,
                                entryCount = data.entries.size,
                                canSubmit = data.myDucks.isNotEmpty() && !isBusy,
                                onSubmitClick = { showSubmitDialog = true }
                            )
                        }
                    }

                    if (data.entries.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Community Feed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShowcaseSortOrder.entries.forEach { order ->
                                        FilterChip(
                                            selected = sortOrder == order,
                                            onClick = { sortOrder = order },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (order == ShowcaseSortOrder.TOP_VOTED) {
                                                        Icons.Rounded.EmojiEvents
                                                    } else {
                                                        Icons.Rounded.Schedule
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text(order.label) },
                                            modifier = Modifier.testTag("showcase_sort_${order.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (displayedEntries.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyPondCard(
                                title = stringResource(R.string.empty_events_title),
                                subtitle = stringResource(R.string.empty_events_subtitle)
                            )
                        }
                    } else {
                        itemsIndexed(displayedEntries, key = { _, e -> e.id }) { _, entry ->
                            val rank = rankMap[entry.id] ?: 1
                            EventEntryCard(
                                rank = rank,
                                entry = entry,
                                isOwn = entry.userId == currentUserId,
                                hasVoted = data.votedEntryIds.contains(entry.id),
                                isBusy = isBusy,
                                onVote = { onCheerEntry(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventHeroBanner(
    event: WeeklyEvent,
    entryCount: Int,
    canSubmit: Boolean,
    onSubmitClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_event_gala_banner),
                    contentDescription = event.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (event.active) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (event.active) "LIVE THIS WEEK" else event.themeTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (event.active) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$entryCount entries",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GrapeClusterIcon(
                            modifier = Modifier.size(18.dp),
                            grapeColor = MaterialTheme.colorScheme.primary,
                            highlightColor = MaterialTheme.colorScheme.primaryContainer,
                            leafColor = MaterialTheme.colorScheme.secondary,
                            contentDesc = null
                        )
                        Text(
                            text = "Vote on community ducks: earn +15 Grapes per vote & send +25 Bonus Grapes to the creator!",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Button(
                    onClick = onSubmitClick,
                    enabled = canSubmit,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_event_duck_button")
                ) {
                    Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit a Duck to Showcase")
                }
            }
        }
    }
}

@Composable
private fun EventEntryCard(
    rank: Int,
    entry: EventEntry,
    isOwn: Boolean,
    hasVoted: Boolean,
    isBusy: Boolean,
    onVote: () -> Unit
) {
    RarityDuckCardContainer(
        rarity = DuckRarity.fromId(entry.duckRarity),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DuckCanvas(
                        palette = DuckPalette.fromMap(entry.ownerAvatar),
                        size = 38.dp,
                        cornerRadius = 12.dp
                    )
                    Column {
                        Text(
                            text = entry.ownerDisplayName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (rank <= 3) "Top #$rank Spotlight" else "Rank #$rank",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (rank <= 3) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Top $rank entry",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DuckCanvas(
                    palette = DuckPalette.fromMap(entry.duckPalette),
                    size = 92.dp,
                    cornerRadius = 18.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.duckName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    RarityBadge(rarity = DuckRarity.fromId(entry.duckRarity))
                    Text(
                        text = entry.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledTonalButton(
                onClick = onVote,
                enabled = !isBusy && !hasVoted && !isOwn,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("vote_entry_${entry.id}")
            ) {
                Icon(
                    imageVector = if (hasVoted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isOwn -> "Your Showcase Duck • ${entry.voteCount} Votes"
                        hasVoted -> "Voted (+15 Earned) • ${entry.voteCount}"
                        else -> "Vote (+15 Grapes) • ${entry.voteCount}"
                    }
                )
            }
        }
    }
}

@Composable
private fun SubmitEventDuckDialog(
    eventTitle: String,
    myDucks: List<RubberDuck>,
    onDismiss: () -> Unit,
    onSubmit: (RubberDuck, String) -> Unit
) {
    var selectedDuck by remember { mutableStateOf(myDucks.firstOrNull()) }
    var caption by remember { mutableStateOf("Ready to make a big splash!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter $eventTitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select a duck from your pond:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myDucks, key = { it.id }) { duck ->
                        val chosen = selectedDuck?.id == duck.id
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (chosen) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.clickable { selectedDuck = duck }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DuckCanvas(
                                    palette = DuckPalette.fromMap(duck.palette),
                                    size = 56.dp,
                                    cornerRadius = 12.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(duck.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 120) caption = it },
                    label = { Text("Showcase caption") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_caption_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duck = selectedDuck ?: return@Button
                    if (caption.isNotBlank()) onSubmit(duck, caption)
                },
                enabled = selectedDuck != null && caption.isNotBlank(),
                modifier = Modifier.testTag("confirm_submit_event_button")
            ) {
                Text("Submit Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
