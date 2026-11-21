package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckTraitCatalog
import com.example.domain.model.TraitOption

private enum class AvatarModuleTab(val label: String, val icon: ImageVector) {
    BODY_PARTS("Body Parts", Icons.Rounded.Extension),
    ACCESSORIES("Accessories", Icons.Rounded.Face),
    COLORS("Colors & Pond", Icons.Rounded.ColorLens)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModularDuckAvatarCustomizer(
    palette: DuckPalette,
    onPaletteChange: (DuckPalette) -> Unit,
    onOpenAiStylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = AvatarModuleTab.entries

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modular Avatar Builder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mix body parts, accessories, and pond colors",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onOpenAiStylist != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onOpenAiStylist,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_ai_stylist_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Stylist")
                }
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("avatar_module_tabs")
        ) {
            tabs.forEachIndexed { index, tab ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    onClick = { selectedTabIndex = index },
                    selected = selectedTabIndex == index,
                    icon = {
                        SegmentedButtonDefaults.Icon(active = selectedTabIndex == index) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("avatar_module_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        when (tabs[selectedTabIndex]) {
            AvatarModuleTab.BODY_PARTS -> {
                ModularTraitSection(
                    title = "Bill Shape",
                    options = DuckTraitCatalog.billStyles,
                    selectedId = palette.billStyle,
                    testTagPrefix = "bill_style",
                    onSelect = { onPaletteChange(palette.copy(billStyle = it)) }
                )
                ModularTraitSection(
                    title = "Wing Style",
                    options = DuckTraitCatalog.wingStyles,
                    selectedId = palette.wingStyle,
                    testTagPrefix = "wing_style",
                    onSelect = { onPaletteChange(palette.copy(wingStyle = it)) }
                )
                ModularTraitSection(
                    title = "Eye Expression",
                    options = DuckTraitCatalog.eyeStyles,
                    selectedId = palette.eyeStyle,
                    testTagPrefix = "eye_style",
                    onSelect = { onPaletteChange(palette.copy(eyeStyle = it)) }
                )
            }

            AvatarModuleTab.ACCESSORIES -> {
                ModularTraitSection(
                    title = "Hats & Toppers",
                    options = DuckTraitCatalog.hats,
                    selectedId = palette.hat,
                    testTagPrefix = "hat",
                    onSelect = { onPaletteChange(palette.copy(hat = it)) }
                )
                ModularTraitSection(
                    title = "Eyewear",
                    options = DuckTraitCatalog.eyewear,
                    selectedId = palette.eyewear,
                    testTagPrefix = "eyewear",
                    onSelect = { onPaletteChange(palette.copy(eyewear = it)) }
                )
                ModularTraitSection(
                    title = "Neck & Floatie Gear",
                    options = DuckTraitCatalog.accessories,
                    selectedId = palette.accessory,
                    testTagPrefix = "accessory",
                    onSelect = { onPaletteChange(palette.copy(accessory = it)) }
                )
            }

            AvatarModuleTab.COLORS -> {
                ModularColorSection(
                    title = "Duck Body Color",
                    options = DuckTraitCatalog.bodyColors,
                    selectedId = palette.bodyColor,
                    testTagPrefix = "body_color",
                    onSelect = { onPaletteChange(palette.copy(bodyColor = it)) }
                )
                ModularColorSection(
                    title = "Bill Color",
                    options = DuckTraitCatalog.billColors,
                    selectedId = palette.billColor,
                    testTagPrefix = "bill_color",
                    onSelect = { onPaletteChange(palette.copy(billColor = it)) }
                )
                ModularColorSection(
                    title = "Pond Backdrop",
                    options = DuckTraitCatalog.pondBackgrounds,
                    selectedId = palette.pondBg,
                    testTagPrefix = "pond_bg",
                    onSelect = { onPaletteChange(palette.copy(pondBg = it)) }
                )
            }
        }

        // Quick Modular Actions (Randomize / Reset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onPaletteChange(DuckTraitCatalog.randomizePalette()) },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("randomize_avatar_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Surprise Me")
            }
            TextButton(
                onClick = { onPaletteChange(DuckPalette.default()) },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("reset_avatar_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }

        // Active Look Summary Pill Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryBadge("Bill: ${labelFor(DuckTraitCatalog.billStyles, palette.billStyle)}")
                SummaryBadge("Wings: ${labelFor(DuckTraitCatalog.wingStyles, palette.wingStyle)}")
                SummaryBadge("Eyes: ${labelFor(DuckTraitCatalog.eyeStyles, palette.eyeStyle)}")
                if (palette.hat != "none") {
                    SummaryBadge("Hat: ${labelFor(DuckTraitCatalog.hats, palette.hat)}")
                }
                if (palette.eyewear != "none") {
                    SummaryBadge("Eyewear: ${labelFor(DuckTraitCatalog.eyewear, palette.eyewear)}")
                }
                if (palette.accessory != "none") {
                    SummaryBadge("Gear: ${labelFor(DuckTraitCatalog.accessories, palette.accessory)}")
                }
                SummaryBadge("Pond: ${labelFor(DuckTraitCatalog.pondBackgrounds, palette.pondBg)}")
            }
        }
    }
}

@Composable
private fun ModularTraitSection(
    title: String,
    options: List<TraitOption>,
    selectedId: String,
    testTagPrefix: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options, key = { it.id }) { option ->
                val isSelected = option.id == selectedId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option.id) },
                    label = { Text(option.label) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("${testTagPrefix}_${option.id}")
                )
            }
        }
    }
}

@Composable
private fun ModularColorSection(
    title: String,
    options: List<TraitOption>,
    selectedId: String,
    testTagPrefix: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(options, key = { it.id }) { option ->
                val selected = option.id == selectedId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            role = Role.RadioButton,
                            onClick = { onSelect(option.id) }
                        )
                        .padding(4.dp)
                        .semantics { contentDescription = "$title ${option.label}" }
                        .testTag("${testTagPrefix}_${option.id}")
                ) {
                    val swatchModifier = if (option.id == "ph_pond") {
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF6B8B), Color(0xFFA855F7), Color(0xFF2EC4B6))
                                )
                            )
                    } else {
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(option.hex))
                    }
                    Box(
                        modifier = swatchModifier.border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            val isDarkSwatch = option.id == "starlight" || option.id == "ph_pond"
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = if (isDarkSwatch) Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(option.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SummaryBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun labelFor(options: List<TraitOption>, id: String): String =
    options.firstOrNull { it.id == id }?.label ?: id
