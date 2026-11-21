package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DuckRarity
import com.example.ui.theme.RarityCommonColor
import com.example.ui.theme.RarityEpicColor
import com.example.ui.theme.RarityLegendaryColor
import com.example.ui.theme.RarityRareColor

enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        fun fromWidth(maxWidth: Dp): WindowWidthClass = when {
            maxWidth < 600.dp -> COMPACT
            maxWidth < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

fun rarityColor(rarity: DuckRarity): Color = when (rarity) {
    DuckRarity.COMMON -> RarityCommonColor
    DuckRarity.RARE -> RarityRareColor
    DuckRarity.EPIC -> RarityEpicColor
    DuckRarity.LEGENDARY -> RarityLegendaryColor
}

@Composable
fun GrapeClusterIcon(
    modifier: Modifier = Modifier,
    grapeColor: Color = Color(0xFF7B2CBF),
    highlightColor: Color = Color(0xFFC77DFF),
    leafColor: Color = Color(0xFF2E8B57),
    contentDesc: String? = "Grapes"
) {
    val semanticsModifier = if (contentDesc != null) {
        modifier.semantics { this.contentDescription = contentDesc }
    } else {
        modifier
    }
    Canvas(modifier = semanticsModifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        // Vine stem & leaf
        drawLine(
            color = leafColor,
            start = Offset(w * 0.50f, h * 0.08f),
            end = Offset(w * 0.50f, h * 0.30f),
            strokeWidth = (w * 0.10f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round
        )
        drawOval(
            color = leafColor,
            topLeft = Offset(w * 0.50f, h * 0.08f),
            size = Size(w * 0.32f, h * 0.18f)
        )

        val r = w * 0.175f
        val berries = listOf(
            // Top row (3)
            Offset(w * 0.28f, h * 0.38f),
            Offset(w * 0.50f, h * 0.36f),
            Offset(w * 0.72f, h * 0.38f),
            // Middle row (2)
            Offset(w * 0.39f, h * 0.58f),
            Offset(w * 0.61f, h * 0.58f),
            // Bottom grape (1)
            Offset(w * 0.50f, h * 0.78f)
        )
        berries.forEach { center ->
            drawCircle(color = grapeColor, radius = r, center = center)
            drawCircle(
                color = highlightColor.copy(alpha = 0.65f),
                radius = r * 0.38f,
                center = Offset(center.x - r * 0.28f, center.y - r * 0.28f)
            )
        }
    }
}

@Composable
fun GrapeAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val primaryGlow = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
    val tertiaryGlow = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.26f)
    val baseBg = MaterialTheme.colorScheme.background
    val grapeClusterTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.075f)
    val leafTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.075f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryGlow,
                        baseBg,
                        tertiaryGlow
                    )
                )
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                // Top-right soft vineyard grape cluster watermark
                val r1 = w * 0.07f
                val topCluster = listOf(
                    Offset(w * 0.84f, h * 0.12f),
                    Offset(w * 0.92f, h * 0.12f),
                    Offset(w * 0.88f, h * 0.18f),
                    Offset(w * 0.96f, h * 0.18f),
                    Offset(w * 0.92f, h * 0.24f)
                )
                topCluster.forEach { c ->
                    drawCircle(color = grapeClusterTint, radius = r1, center = c)
                }
                drawOval(
                    color = leafTint,
                    topLeft = Offset(w * 0.84f, h * 0.04f),
                    size = Size(w * 0.14f, w * 0.07f)
                )

                // Bottom-left soft grape cluster watermark
                val r2 = w * 0.06f
                val bottomCluster = listOf(
                    Offset(w * 0.06f, h * 0.82f),
                    Offset(w * 0.13f, h * 0.82f),
                    Offset(w * 0.095f, h * 0.87f),
                    Offset(w * 0.165f, h * 0.87f),
                    Offset(w * 0.13f, h * 0.92f)
                )
                bottomCluster.forEach { c ->
                    drawCircle(color = grapeClusterTint, radius = r2, center = c)
                }
                drawOval(
                    color = leafTint,
                    topLeft = Offset(w * 0.06f, h * 0.75f),
                    size = Size(w * 0.12f, w * 0.06f)
                )
            },
        content = content
    )
}

@Composable
fun RarityBadge(rarity: DuckRarity, modifier: Modifier = Modifier) {
    val tint = rarityColor(rarity)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (rarity == DuckRarity.EPIC || rarity == DuckRarity.LEGENDARY) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = rarity.label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RarityDuckCardContainer(
    rarity: DuckRarity,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 22.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseColor = rarityColor(rarity)

    val needsGlowAnimation = rarity == DuckRarity.EPIC || rarity == DuckRarity.LEGENDARY
    val glowPulse = if (needsGlowAnimation) {
        val transition = rememberInfiniteTransition(label = "rarity_glow_${rarity.id}")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (rarity == DuckRarity.LEGENDARY) 2000 else 2600,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rarity_pulse"
        )
        animated
    } else {
        0.5f
    }

    val borderWidth = when (rarity) {
        DuckRarity.COMMON -> if (selected) 2.dp else 1.25.dp
        DuckRarity.RARE -> if (selected) 2.25.dp else 1.75.dp
        DuckRarity.EPIC -> if (selected) 2.75.dp else 2.25.dp
        DuckRarity.LEGENDARY -> if (selected) 3.25.dp else 2.75.dp
    }

    val borderBrush = when (rarity) {
        DuckRarity.COMMON -> Brush.linearGradient(
            colors = listOf(
                baseColor.copy(alpha = if (selected) 0.9f else 0.55f),
                baseColor.copy(alpha = if (selected) 0.75f else 0.35f)
            )
        )
        DuckRarity.RARE -> Brush.linearGradient(
            colors = listOf(
                baseColor,
                Color(0xFF80CBC4),
                baseColor
            )
        )
        DuckRarity.EPIC -> Brush.linearGradient(
            colors = listOf(
                baseColor,
                Color(0xFF9D4EDD),
                Color(0xFFF472B6),
                baseColor
            )
        )
        DuckRarity.LEGENDARY -> Brush.linearGradient(
            colors = listOf(
                baseColor,
                Color(0xFFFFE082),
                Color(0xFFFFB300),
                baseColor
            ),
            start = Offset(glowPulse * 140f, 0f),
            end = Offset(260f + glowPulse * 140f, 340f)
        )
    }

    val auraAlpha = when (rarity) {
        DuckRarity.COMMON -> if (selected) 0.16f else 0.08f
        DuckRarity.RARE -> if (selected) 0.28f else 0.18f
        DuckRarity.EPIC -> 0.24f + glowPulse * 0.16f
        DuckRarity.LEGENDARY -> 0.30f + glowPulse * 0.20f
    }

    val shadowElevation = when (rarity) {
        DuckRarity.COMMON -> if (selected) 4.dp else 1.dp
        DuckRarity.RARE -> if (selected) 6.dp else 4.dp
        DuckRarity.EPIC -> 8.dp
        DuckRarity.LEGENDARY -> 12.dp
    }

    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val innerSheen = when (rarity) {
        DuckRarity.COMMON -> Color.Transparent
        DuckRarity.RARE -> baseColor.copy(alpha = 0.05f)
        DuckRarity.EPIC -> baseColor.copy(alpha = 0.08f)
        DuckRarity.LEGENDARY -> baseColor.copy(alpha = 0.11f)
    }

    val cardModifier = modifier
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            val haloSpread = when (rarity) {
                DuckRarity.COMMON -> 2.dp.toPx()
                DuckRarity.RARE -> 4.dp.toPx()
                DuckRarity.EPIC -> 6.dp.toPx()
                DuckRarity.LEGENDARY -> 8.dp.toPx()
            }
            if (rarity != DuckRarity.COMMON || selected) {
                drawRoundRect(
                    color = baseColor.copy(alpha = auraAlpha * 0.45f),
                    topLeft = Offset(-haloSpread, -haloSpread),
                    size = Size(size.width + haloSpread * 2f, size.height + haloSpread * 2f),
                    cornerRadius = CornerRadius(radiusPx + haloSpread, radiusPx + haloSpread)
                )
            }
            drawRoundRect(
                color = baseColor.copy(alpha = auraAlpha),
                topLeft = Offset(-haloSpread * 0.45f, -haloSpread * 0.45f),
                size = Size(size.width + haloSpread * 0.9f, size.height + haloSpread * 0.9f),
                cornerRadius = CornerRadius(radiusPx + haloSpread * 0.45f, radiusPx + haloSpread * 0.45f)
            )
        }
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = baseColor,
            spotColor = baseColor
        )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderBrush),
            modifier = cardModifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(innerSheen, Color.Transparent)
                        )
                    ),
                content = content
            )
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, borderBrush),
            modifier = cardModifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(innerSheen, Color.Transparent)
                        )
                    ),
                content = content
            )
        }
    }
}

@Composable
fun PondCoinsChip(
    coins: Long,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GrapeClusterIcon(
                modifier = Modifier.size(18.dp),
                grapeColor = MaterialTheme.colorScheme.primary,
                highlightColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
                leafColor = MaterialTheme.colorScheme.secondary,
                contentDesc = "Grapes"
            )
            Text(
                text = "$coins",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyPondCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_pond),
                contentDescription = title,
                modifier = Modifier
                    .size(132.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NotificationBellChip(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = "Open notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}
