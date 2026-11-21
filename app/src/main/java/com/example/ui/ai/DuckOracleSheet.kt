package com.example.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckRarity
import com.example.domain.model.RubberDuck
import com.example.ui.components.DuckCanvas
import com.example.ui.components.GrapeAppBackground
import com.example.ui.components.RarityBadge
import com.example.ui.components.WindowWidthClass

@Composable
fun DuckOracleSheet(
    initialTab: Int,
    myDucks: List<RubberDuck>,
    coins: Long,
    activeEventTitle: String?,
    selectedLoreDuckId: String?,
    chatState: OracleChatUiState,
    loreState: DuckLoreUiState,
    stylistState: AvatarStylistUiState,
    onAskOracle: (String) -> Unit,
    onGenerateLore: (RubberDuck) -> Unit,
    onSuggestAvatar: (String) -> Unit,
    onApplyAvatarSuggestion: (DuckPalette) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Pond Oracle Chat", "Duck Lore", "Avatar Stylist")

    GrapeAppBackground {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("duck_oracle_screen")
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = WindowWidthClass.fromWidth(maxWidth) == WindowWidthClass.EXPANDED

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = if (isWide) 920.dp else 640.dp)
                    .align(Alignment.TopCenter)
            ) {
                item {
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
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
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
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Pond AI Oracle",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Powered by Gemini • Trade advice, duck lore & avatar styling",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier.testTag("close_oracle_button")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close Pond Oracle")
                                }
                            }
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tabs.indices.toList()) { idx ->
                            FilterChip(
                                selected = activeTab == idx,
                                onClick = { activeTab = idx },
                                label = { Text(tabs[idx]) },
                                modifier = Modifier.testTag("oracle_tab_$idx")
                            )
                        }
                    }
                }

                when (activeTab) {
                    0 -> item {
                        OracleChatSection(
                            chatState = chatState,
                            activeEventTitle = activeEventTitle,
                            coins = coins,
                            duckCount = myDucks.size,
                            onAsk = onAskOracle
                        )
                    }

                    1 -> item {
                        DuckLoreSection(
                            myDucks = myDucks,
                            initialDuckId = selectedLoreDuckId,
                            loreState = loreState,
                            onGenerateLore = onGenerateLore
                        )
                    }

                    2 -> item {
                        AvatarStylistSection(
                            stylistState = stylistState,
                            onSuggest = onSuggestAvatar,
                            onApply = onApplyAvatarSuggestion
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun OracleChatSection(
    chatState: OracleChatUiState,
    activeEventTitle: String?,
    coins: Long,
    duckCount: Int,
    onAsk: (String) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }
    val quickPrompts = remember(activeEventTitle) {
        listOf(
            "Rate my flock's rarity & value",
            "How should I invest my $coins Grapes?",
            if (!activeEventTitle.isNullOrBlank()) "Tips to win $activeEventTitle" else "Tips for weekly pond events",
            "Write a short duck pond poem"
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ask the Pond Oracle ($duckCount ducks • $coins Grapes)",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickPrompts) { sample ->
                    AssistChip(
                        onClick = {
                            promptInput = sample
                            onAsk(sample)
                        },
                        label = { Text(sample) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { if (it.length <= 300) promptInput = it },
                    placeholder = { Text("Ask about trades, ducks, or events...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("oracle_chat_input")
                )
                Button(
                    onClick = {
                        onAsk(promptInput)
                        promptInput = ""
                    },
                    enabled = chatState !is OracleChatUiState.Loading && chatState !is OracleChatUiState.Streaming && promptInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("oracle_chat_send_button")
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = "Ask Pond Oracle")
                }
            }

            when (chatState) {
                is OracleChatUiState.Idle -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tap a suggestion above or ask anything about your rubber duck collection!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is OracleChatUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Consulting the Pond Oracle...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is OracleChatUiState.Streaming -> {
                    OracleResponseCard(prompt = chatState.prompt, body = chatState.text, isStreaming = true)
                }

                is OracleChatUiState.Success -> {
                    OracleResponseCard(prompt = chatState.prompt, body = chatState.text, isStreaming = false)
                }

                is OracleChatUiState.Error -> {
                    OracleErrorCard(message = chatState.message)
                }
            }
        }
    }
}

@Composable
private fun OracleResponseCard(
    prompt: String,
    body: String,
    isStreaming: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("oracle_response_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Q: $prompt",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isStreaming) "$body ▍" else body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DuckLoreSection(
    myDucks: List<RubberDuck>,
    initialDuckId: String?,
    loreState: DuckLoreUiState,
    onGenerateLore: (RubberDuck) -> Unit
) {
    var selectedDuckId by remember(myDucks, initialDuckId) {
        mutableStateOf(
            myDucks.firstOrNull { it.id == initialDuckId }?.id
                ?: myDucks.firstOrNull()?.id
        )
    }
    val selectedDuck = myDucks.firstOrNull { it.id == selectedDuckId }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Duck Backstory & Showcase Writer", style = MaterialTheme.typography.titleMedium)

            if (selectedDuck == null) {
                Text(
                    text = "Hatch or collect a duck first to generate its Gemini backstory and event caption!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(myDucks, key = { it.id }) { duck ->
                        FilterChip(
                            selected = duck.id == selectedDuck.id,
                            onClick = { selectedDuckId = duck.id },
                            label = { Text(duck.name) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DuckCanvas(
                        palette = DuckPalette.fromMap(selectedDuck.palette),
                        size = 84.dp,
                        cornerRadius = 18.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selectedDuck.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        RarityBadge(rarity = DuckRarity.fromId(selectedDuck.rarity))
                    }
                }

                Button(
                    onClick = { onGenerateLore(selectedDuck) },
                    enabled = loreState !is DuckLoreUiState.Loading,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_duck_lore_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Lore & Event Caption")
                }

                when (loreState) {
                    is DuckLoreUiState.Idle -> Unit
                    is DuckLoreUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Text("Writing duck legend...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is DuckLoreUiState.Success -> {
                        val lore = loreState.lore
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duck_lore_result_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = lore.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = lore.backstory, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Event Caption: \"${lore.eventCaption}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Trade Tip: ${lore.tradeTip}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DuckLoreUiState.Error -> {
                        OracleErrorCard(message = loreState.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarStylistSection(
    stylistState: AvatarStylistUiState,
    onSuggest: (String) -> Unit,
    onApply: (DuckPalette) -> Unit
) {
    var vibeInput by remember { mutableStateOf("Cozy starry night scholar") }
    val presetVibes = listOf(
        "Cozy starry night scholar",
        "Royal golden pond monarch",
        "Beach vacation surfer",
        "Pastel spring picnic"
    )

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
                Icon(Icons.Rounded.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("AI Avatar Stylist", style = MaterialTheme.typography.titleMedium)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presetVibes) { vibe ->
                    AssistChip(
                        onClick = {
                            vibeInput = vibe
                            onSuggest(vibe)
                        },
                        label = { Text(vibe) }
                    )
                }
            }

            OutlinedTextField(
                value = vibeInput,
                onValueChange = { if (it.length <= 120) vibeInput = it },
                label = { Text("Describe your duck's vibe") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("avatar_stylist_input")
            )

            Button(
                onClick = { onSuggest(vibeInput) },
                enabled = stylistState !is AvatarStylistUiState.Loading && vibeInput.isNotBlank(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_avatar_style_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Style My Duck Avatar")
            }

            when (stylistState) {
                is AvatarStylistUiState.Idle -> Unit
                is AvatarStylistUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Text("Designing avatar outfit...", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                is AvatarStylistUiState.Success -> {
                    val suggestion = stylistState.suggestion
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("avatar_stylist_result_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DuckCanvas(
                                palette = suggestion.palette,
                                size = 128.dp,
                                cornerRadius = 24.dp
                            )
                            Text(
                                text = suggestion.styleTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = suggestion.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { onApply(suggestion.palette) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("apply_ai_avatar_button")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Apply Look to My Avatar")
                            }
                        }
                    }
                }

                is AvatarStylistUiState.Error -> {
                    OracleErrorCard(message = stylistState.message)
                }
            }
        }
    }
}

@Composable
private fun OracleErrorCard(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("oracle_error_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
