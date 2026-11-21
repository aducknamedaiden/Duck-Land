package com.example.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckSpeciesCatalog
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.UiState
import com.example.ui.components.DuckCanvas
import com.example.ui.components.EmptyPondCard
import com.example.ui.components.GrapeClusterIcon
import com.example.ui.components.WindowWidthClass

@Composable
fun LeaderboardScreen(
    currentUserId: String,
    isOwner: Boolean,
    selectedSort: LeaderboardSort,
    uiState: UiState<List<LeaderboardEntry>>,
    onSelectSort: (LeaderboardSort) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = WindowWidthClass.fromWidth(maxWidth)
        val contentMaxWidth = if (widthClass == WindowWidthClass.EXPANDED) 920.dp else 640.dp

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
                        title = "Leaderboard unavailable",
                        subtitle = uiState.message
                    )
                }
            }

            is UiState.Success -> {
                val entries = uiState.data
                val myIndex = entries.indexOfFirst { it.userId == currentUserId }
                val totalSpeciesCatalog = DuckSpeciesCatalog.all.size

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = contentMaxWidth)
                        .align(Alignment.TopCenter)
                        .testTag("leaderboard_list")
                ) {
                    item {
                        LeaderboardHeaderCard(
                            collectorCount = entries.size,
                            totalSpeciesCatalog = totalSpeciesCatalog
                        )
                    }

                    if (isOwner) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("leaderboard_admin_hidden_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Owner/Admin accounts are excluded from public rankings so collectors compete fairly. Collectors you gift Grapes or ducks to still appear here!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    } else if (myIndex >= 0) {
                        item {
                            MyLeaderboardStandingCard(
                                rank = myIndex + 1,
                                entry = entries[myIndex],
                                totalSpeciesCatalog = totalSpeciesCatalog
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(LeaderboardSort.entries) { sortOption ->
                                FilterChip(
                                    selected = selectedSort == sortOption,
                                    onClick = { onSelectSort(sortOption) },
                                    label = { Text(sortOption.label) },
                                    modifier = Modifier.testTag("leaderboard_sort_${sortOption.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        item {
                            EmptyPondCard(
                                title = "No ranked collectors yet",
                                subtitle = "Hatch ducks, customize your avatar, and trade with friends to claim the #1 pond spot!"
                            )
                        }
                    } else {
                        if (entries.size >= 3) {
                            item {
                                PodiumRowCard(
                                    topThree = entries.take(3),
                                    currentUserId = currentUserId,
                                    sort = selectedSort
                                )
                            }
                        }

                        itemsIndexed(entries, key = { _, item -> item.userId }) { index, entry ->
                            LeaderboardCollectorRow(
                                rank = index + 1,
                                entry = entry,
                                isMe = entry.userId == currentUserId,
                                totalSpeciesCatalog = totalSpeciesCatalog
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardHeaderCard(
    collectorCount: Int,
    totalSpeciesCatalog: Int
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Pond Leaderboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = "$collectorCount active collectors competing across $totalSpeciesCatalog duck species.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFB703),
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
private fun MyLeaderboardStandingCard(
    rank: Int,
    entry: LeaderboardEntry,
    totalSpeciesCatalog: Int
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_leaderboard_standing")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankBadgeCircle(rank = rank)
            DuckCanvas(
                palette = DuckPalette.fromMap(entry.avatarConfig),
                size = 52.dp,
                cornerRadius = 16.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Rank • #$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.duckCount} ducks • ${entry.uniqueSpeciesCount}/$totalSpeciesCatalog species • ${entry.tradesCompleted} trades",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PodiumRowCard(
    topThree: List<LeaderboardEntry>,
    currentUserId: String,
    sort: LeaderboardSort
) {
    val orderedPodium = listOf(1 to topThree[1], 0 to topThree[0], 2 to topThree[2])

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Top Pond Champions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                orderedPodium.forEach { (idx, entry) ->
                    val rank = idx + 1
                    val medalColor = when (rank) {
                        1 -> Color(0xFFFFB703)
                        2 -> Color(0xFF94A3B8)
                        else -> Color(0xFFCD7F32)
                    }
                    val metricLabel = when (sort) {
                        LeaderboardSort.TOTAL_DUCKS -> "${entry.duckCount} ducks"
                        LeaderboardSort.UNIQUE_SPECIES -> "${entry.uniqueSpeciesCount} species"
                        LeaderboardSort.TRADES -> "${entry.tradesCompleted} trades"
                        LeaderboardSort.GRAPES -> "${entry.grapeBalance} Grapes"
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (entry.userId == currentUserId) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(1.5.dp, medalColor.copy(alpha = 0.7f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = medalColor,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#$rank",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1F1602),
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            DuckCanvas(
                                palette = DuckPalette.fromMap(entry.avatarConfig),
                                size = if (rank == 1) 68.dp else 56.dp,
                                cornerRadius = 18.dp
                            )
                            Text(
                                text = entry.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = metricLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardCollectorRow(
    rank: Int,
    entry: LeaderboardEntry,
    isMe: Boolean,
    totalSpeciesCatalog: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        border = if (isMe) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.userId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankBadgeCircle(rank = rank)
            DuckCanvas(
                palette = DuckPalette.fromMap(entry.avatarConfig),
                size = 54.dp,
                cornerRadius = 16.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isMe) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatMiniPill(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        text = "${entry.uniqueSpeciesCount}/$totalSpeciesCatalog sp."
                    )
                    StatMiniPill(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = "${entry.tradesCompleted}"
                    )
                    StatMiniPill(
                        icon = {
                            GrapeClusterIcon(
                                modifier = Modifier.size(13.dp),
                                grapeColor = MaterialTheme.colorScheme.primary,
                                highlightColor = MaterialTheme.colorScheme.primaryContainer,
                                leafColor = MaterialTheme.colorScheme.secondary,
                                contentDesc = null
                            )
                        },
                        text = "${entry.grapeBalance}"
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.duckCount}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (entry.duckCount == 1L) "duck" else "ducks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RankBadgeCircle(rank: Int) {
    val bgColor = when (rank) {
        1 -> Color(0xFFFFD54F)
        2 -> Color(0xFFE2E8F0)
        3 -> Color(0xFFE6B89C)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (rank) {
        1, 2, 3 -> Color(0xFF1E293B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun StatMiniPill(
    icon: @Composable () -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        icon()
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
