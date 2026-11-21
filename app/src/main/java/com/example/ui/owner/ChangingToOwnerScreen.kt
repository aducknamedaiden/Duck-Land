package com.example.ui.owner

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DuckPalette
import com.example.ui.components.DuckCanvas
import kotlinx.coroutines.delay

@Composable
fun ChangingToOwnerScreen(
    onFinished: () -> Unit
) {
    BackHandler { /* keep transition uninterrupted */ }

    var phase by remember { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            "Verifying *OWNERDUCK#1* Royal Seal...",
            "Granting Duck Spawner & Grape Mint...",
            "Enabling Pond Moderation Console...",
            "OWNER STATUS ACTIVE!"
        )
    }

    LaunchedEffect(Unit) {
        delay(650L)
        phase = 1
        delay(700L)
        phase = 2
        delay(750L)
        phase = 3
        delay(700L)
        onFinished()
    }

    val progressTarget = when (phase) {
        0 -> 0.28f
        1 -> 0.62f
        2 -> 0.88f
        else -> 1f
    }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "ownerProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ownerPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "duckPulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing)
        ),
        label = "ringRotation"
    )

    val ownerPalette = remember {
        DuckPalette(
            bodyColor = "golden_glow",
            billColor = "coral_orange",
            hat = "crown",
            eyewear = "monocle",
            accessory = "medal",
            pondBg = "starlight"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0D2E),
                        Color(0xFF2D154B),
                        Color(0xFF281F12)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
            .testTag("changing_to_owner_screen"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val baseRadius = size.minDimension * 0.24f
            listOf(1.0f, 1.38f, 1.78f).forEachIndexed { idx, mult ->
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = (0.26f - idx * 0.07f)),
                    radius = baseRadius * mult * (0.95f + (pulse - 0.94f)),
                    center = center,
                    style = Stroke(width = (4 - idx).dp.toPx())
                )
            }
            for (i in 0 until 8) {
                val angle = Math.toRadians((ringRotation + i * 45.0))
                val rx = center.x + kotlin.math.cos(angle).toFloat() * baseRadius * 1.38f
                val ry = center.y + kotlin.math.sin(angle).toFloat() * baseRadius * 1.38f
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = 0.75f),
                    radius = 4.dp.toPx(),
                    center = Offset(rx, ry)
                )
            }
        }

        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFD54F).copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PRIVILEGE OVERRIDE • *OWNERDUCK#1*",
                        color = Color(0xFFFFE082),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(modifier = Modifier.scale(pulse)) {
                DuckCanvas(
                    palette = ownerPalette,
                    size = 168.dp,
                    cornerRadius = 38.dp
                )
            }

            Text(
                text = "CHANGING TO OWNER",
                color = Color(0xFFFFD54F),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = steps[phase],
                color = Color(0xFFEAF6F5),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                )
                steps.forEachIndexed { idx, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (idx <= phase) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = if (idx <= phase) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = label,
                            color = if (idx <= phase) Color.White else Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onFinished,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD54F),
                    contentColor = Color(0xFF1B1404)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("enter_owner_panel_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter Owner Panel", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
