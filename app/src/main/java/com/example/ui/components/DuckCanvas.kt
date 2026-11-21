package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.DuckPalette
import com.example.domain.model.DuckTraitCatalog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DuckCanvas(
    palette: DuckPalette,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    cornerRadius: Dp = 24.dp,
    showPondBackground: Boolean = true
) {
    val bodyColor = Color(
        DuckTraitCatalog.bodyColors.firstOrNull { it.id == palette.bodyColor }?.hex ?: 0xFFFFD54FL
    )
    val billColor = Color(
        DuckTraitCatalog.billColors.firstOrNull { it.id == palette.billColor }?.hex ?: 0xFFFF7A45L
    )
    val bgHex = DuckTraitCatalog.pondBackgrounds.firstOrNull { it.id == palette.pondBg }?.hex ?: 0xFFCBEFE9L
    val bgColor = Color(bgHex)

    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        val w = this.size.width
        val h = this.size.height

        if (showPondBackground) {
            drawPondBackdrop(palette.pondBg, bgColor, w, h)
        }

        // Pool floatie behind lower body if selected
        if (palette.accessory == "floatie") {
            drawFloatieBack(w, h)
        }

        // Duck body (chubby rubber duck silhouette)
        val bodyLeft = w * 0.18f
        val bodyTop = h * 0.48f
        val bodyW = w * 0.64f
        val bodyH = h * 0.36f

        // Tail curve + body oval
        val tailPath = Path().apply {
            moveTo(w * 0.24f, h * 0.62f)
            quadraticTo(w * 0.10f, h * 0.50f, w * 0.15f, h * 0.45f)
            quadraticTo(w * 0.25f, h * 0.49f, w * 0.34f, h * 0.54f)
            close()
        }
        drawPath(tailPath, color = darken(bodyColor, 0.06f))
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(lighten(bodyColor, 0.15f), bodyColor, darken(bodyColor, 0.10f)),
                startY = bodyTop,
                endY = bodyTop + bodyH
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH)
        )

        // Modular Wing Style
        drawDuckWing(palette.wingStyle, bodyColor, w, h)

        // Floatie front rim
        if (palette.accessory == "floatie") {
            drawFloatieFront(w, h)
        }

        // Duck Head
        val headSize = w * 0.40f
        val headCenter = Offset(w * 0.54f, h * 0.38f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lighten(bodyColor, 0.25f), bodyColor, darken(bodyColor, 0.08f)),
                center = Offset(headCenter.x - headSize * 0.12f, headCenter.y - headSize * 0.15f),
                radius = headSize * 0.75f
            ),
            radius = headSize / 2f,
            center = headCenter
        )

        // Glossy rubber highlight on head
        drawOval(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset(headCenter.x - headSize * 0.26f, headCenter.y - headSize * 0.36f),
            size = Size(headSize * 0.24f, headSize * 0.12f)
        )

        // Neck Accessories
        when (palette.accessory) {
            "bowtie" -> drawBowtie(w, h)
            "scarf" -> drawScarf(w, h)
            "medal" -> drawMedal(w, h)
        }

        // Cute rosy cheek
        drawOval(
            color = Color(0xFFFF758F).copy(alpha = 0.42f),
            topLeft = Offset(w * 0.48f, h * 0.41f),
            size = Size(w * 0.09f, h * 0.055f)
        )

        // Modular Duck Bill
        drawDuckBill(palette.billStyle, billColor, w, h)

        // Modular Expressive Eye
        val eyeCenter = Offset(w * 0.59f, h * 0.34f)
        drawDuckEye(palette.eyeStyle, eyeCenter, w)

        // Eyewear
        when (palette.eyewear) {
            "shades" -> drawShades(eyeCenter, w)
            "round" -> drawRoundGlasses(eyeCenter, w)
            "star" -> drawStarSpecs(eyeCenter, w)
            "monocle" -> drawMonocle(eyeCenter, w)
        }

        // Hats
        when (palette.hat) {
            "crown" -> drawCrown(headCenter, headSize, w)
            "sailor" -> drawSailorCap(headCenter, headSize, w)
            "beanie" -> drawBeanie(headCenter, headSize, w)
            "sprout" -> drawSprout(headCenter, headSize, w)
            "halo" -> drawHalo(headCenter, headSize, w)
        }
    }
}

private fun DrawScope.drawPondBackdrop(bgId: String, baseColor: Color, w: Float, h: Float) {
    when (bgId) {
        "grape_pond" -> {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3E7FF),
                        Color(0xFFDEC3FF),
                        Color(0xFFBB8EF7)
                    )
                )
            )
            // Soft floating grape bubble cluster in upper-left corner
            val grapeBubble = Color(0xFF7B2CBF).copy(alpha = 0.16f)
            val bubbleHighlight = Color(0xFFFFFFFF).copy(alpha = 0.45f)
            listOf(
                Offset(w * 0.17f, h * 0.18f) to w * 0.055f,
                Offset(w * 0.25f, h * 0.19f) to w * 0.05f,
                Offset(w * 0.21f, h * 0.25f) to w * 0.052f,
                Offset(w * 0.84f, h * 0.24f) to w * 0.042f,
                Offset(w * 0.89f, h * 0.30f) to w * 0.032f
            ).forEach { (center, radius) ->
                drawCircle(grapeBubble, radius = radius, center = center)
                drawCircle(
                    bubbleHighlight,
                    radius = radius * 0.32f,
                    center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f)
                )
            }
            // Tiny green vine leaf accent
            drawOval(
                color = Color(0xFF2E8B57).copy(alpha = 0.28f),
                topLeft = Offset(w * 0.18f, h * 0.09f),
                size = Size(w * 0.11f, h * 0.055f)
            )
            val rippleColor = Color.White.copy(alpha = 0.6f)
            drawOval(
                color = rippleColor,
                topLeft = Offset(w * 0.12f, h * 0.76f),
                size = Size(w * 0.76f, h * 0.12f),
                style = Stroke(width = w * 0.018f)
            )
            drawOval(
                color = rippleColor.copy(alpha = 0.38f),
                topLeft = Offset(w * 0.22f, h * 0.80f),
                size = Size(w * 0.56f, h * 0.08f),
                style = Stroke(width = w * 0.014f)
            )
        }

        "ph_pond" -> {
            // pH litmus spectrum: acidic berry-rose -> neutral purple-violet -> alkaline teal-aqua
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B8B),
                        Color(0xFFA855F7),
                        Color(0xFF2EC4B6)
                    )
                )
            )
            // Subtle pH spectrum indicator dots along the left edge
            val scaleColors = listOf(
                Color(0xFFFFB3C6),
                Color(0xFFE879F9),
                Color(0xFFC084FC),
                Color(0xFF67E8F9)
            )
            scaleColors.forEachIndexed { idx, tint ->
                drawCircle(
                    color = tint.copy(alpha = 0.75f),
                    radius = w * 0.022f,
                    center = Offset(w * 0.11f, h * (0.16f + idx * 0.10f))
                )
            }
            val rippleColor = Color(0xFFE0F7FA).copy(alpha = 0.52f)
            drawOval(
                color = rippleColor,
                topLeft = Offset(w * 0.12f, h * 0.76f),
                size = Size(w * 0.76f, h * 0.12f),
                style = Stroke(width = w * 0.018f)
            )
            drawOval(
                color = rippleColor.copy(alpha = 0.32f),
                topLeft = Offset(w * 0.22f, h * 0.80f),
                size = Size(w * 0.56f, h * 0.08f),
                style = Stroke(width = w * 0.014f)
            )
        }

        else -> {
            val isDarkPond = bgId == "starlight"
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lighten(baseColor, if (isDarkPond) 0.08f else 0.12f),
                        baseColor,
                        darken(baseColor, if (isDarkPond) 0.16f else 0.06f)
                    )
                )
            )
            if (isDarkPond) {
                val starColor = Color(0xFFFFE082).copy(alpha = 0.8f)
                drawCircle(starColor, radius = w * 0.016f, center = Offset(w * 0.18f, h * 0.20f))
                drawCircle(starColor, radius = w * 0.012f, center = Offset(w * 0.80f, h * 0.16f))
                drawCircle(starColor, radius = w * 0.014f, center = Offset(w * 0.28f, h * 0.34f))
            }
            // Soft water ripples under duck
            val rippleColor = if (isDarkPond) Color(0xFF5CE0CE).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.55f)
            drawOval(
                color = rippleColor,
                topLeft = Offset(w * 0.12f, h * 0.76f),
                size = Size(w * 0.76f, h * 0.12f),
                style = Stroke(width = w * 0.018f)
            )
            drawOval(
                color = rippleColor.copy(alpha = rippleColor.alpha * 0.65f),
                topLeft = Offset(w * 0.22f, h * 0.80f),
                size = Size(w * 0.56f, h * 0.08f),
                style = Stroke(width = w * 0.014f)
            )
        }
    }
}

private fun DrawScope.drawFloatieBack(w: Float, h: Float) {
    drawOval(
        color = Color(0xFFFF758F),
        topLeft = Offset(w * 0.14f, h * 0.63f),
        size = Size(w * 0.72f, h * 0.23f)
    )
}

private fun DrawScope.drawFloatieFront(w: Float, h: Float) {
    drawArc(
        color = Color(0xFFFF5C8A),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.15f, h * 0.65f),
        size = Size(w * 0.70f, h * 0.18f),
        style = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color.White.copy(alpha = 0.75f),
        startAngle = 25f,
        sweepAngle = 35f,
        useCenter = false,
        topLeft = Offset(w * 0.15f, h * 0.65f),
        size = Size(w * 0.70f, h * 0.18f),
        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawBowtie(w: Float, h: Float) {
    val cx = w * 0.57f
    val cy = h * 0.55f
    val leftTri = Path().apply {
        moveTo(cx, cy)
        lineTo(cx - w * 0.08f, cy - h * 0.045f)
        lineTo(cx - w * 0.08f, cy + h * 0.045f)
        close()
    }
    val rightTri = Path().apply {
        moveTo(cx, cy)
        lineTo(cx + w * 0.08f, cy - h * 0.045f)
        lineTo(cx + w * 0.08f, cy + h * 0.045f)
        close()
    }
    drawPath(leftTri, Color(0xFFE55D7B))
    drawPath(rightTri, Color(0xFFE55D7B))
    drawCircle(Color(0xFFFF99AC), radius = w * 0.025f, center = Offset(cx, cy))
}

private fun DrawScope.drawScarf(w: Float, h: Float) {
    drawRoundRect(
        color = Color(0xFF1A9385),
        topLeft = Offset(w * 0.39f, h * 0.50f),
        size = Size(w * 0.30f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawRoundRect(
        color = Color(0xFF2EC4B6),
        topLeft = Offset(w * 0.53f, h * 0.53f),
        size = Size(w * 0.09f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
}

private fun DrawScope.drawMedal(w: Float, h: Float) {
    drawLine(
        color = Color(0xFFE55D7B),
        start = Offset(w * 0.50f, h * 0.52f),
        end = Offset(w * 0.56f, h * 0.62f),
        strokeWidth = w * 0.028f
    )
    drawLine(
        color = Color(0xFFE55D7B),
        start = Offset(w * 0.62f, h * 0.52f),
        end = Offset(w * 0.56f, h * 0.62f),
        strokeWidth = w * 0.028f
    )
    drawCircle(Color(0xFFFFB703), radius = w * 0.055f, center = Offset(w * 0.56f, h * 0.64f))
    drawCircle(Color(0xFFFFE082), radius = w * 0.032f, center = Offset(w * 0.56f, h * 0.64f))
}

private fun DrawScope.drawShades(eyeCenter: Offset, w: Float) {
    drawRoundRect(
        color = Color(0xFF0F3D3E),
        topLeft = Offset(eyeCenter.x - w * 0.07f, eyeCenter.y - w * 0.045f),
        size = Size(w * 0.14f, w * 0.085f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawLine(
        color = Color(0xFF0F3D3E),
        start = Offset(eyeCenter.x - w * 0.15f, eyeCenter.y - w * 0.01f),
        end = Offset(eyeCenter.x - w * 0.06f, eyeCenter.y - w * 0.02f),
        strokeWidth = w * 0.02f
    )
    drawLine(
        color = Color(0xFF5CE0CE).copy(alpha = 0.7f),
        start = Offset(eyeCenter.x - w * 0.04f, eyeCenter.y - w * 0.025f),
        end = Offset(eyeCenter.x + w * 0.02f, eyeCenter.y + w * 0.02f),
        strokeWidth = w * 0.015f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawRoundGlasses(eyeCenter: Offset, w: Float) {
    drawCircle(
        color = Color(0xFF334155),
        radius = w * 0.058f,
        center = eyeCenter,
        style = Stroke(width = w * 0.016f)
    )
    drawLine(
        color = Color(0xFF334155),
        start = Offset(eyeCenter.x - w * 0.15f, eyeCenter.y),
        end = Offset(eyeCenter.x - w * 0.058f, eyeCenter.y),
        strokeWidth = w * 0.014f
    )
}

private fun DrawScope.drawStarSpecs(eyeCenter: Offset, w: Float) {
    val starPath = Path()
    val outer = w * 0.07f
    val inner = w * 0.032f
    for (i in 0 until 10) {
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val r = if (i % 2 == 0) outer else inner
        val px = eyeCenter.x + (r * cos(angle)).toFloat()
        val py = eyeCenter.y + (r * sin(angle)).toFloat()
        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
    }
    starPath.close()
    drawPath(starPath, Color(0xFFAB47BC))
    drawPath(starPath, Color(0xFFFFD166), style = Stroke(width = w * 0.012f, join = StrokeJoin.Round))
}

private fun DrawScope.drawMonocle(eyeCenter: Offset, w: Float) {
    drawCircle(
        color = Color(0xFFFFB703),
        radius = w * 0.056f,
        center = eyeCenter,
        style = Stroke(width = w * 0.018f)
    )
    drawLine(
        color = Color(0xFFFFB703),
        start = Offset(eyeCenter.x - w * 0.04f, eyeCenter.y + w * 0.04f),
        end = Offset(eyeCenter.x - w * 0.08f, eyeCenter.y + w * 0.18f),
        strokeWidth = w * 0.012f
    )
}

private fun DrawScope.drawCrown(headCenter: Offset, headSize: Float, w: Float) {
    val baseY = headCenter.y - headSize * 0.42f
    val crownPath = Path().apply {
        moveTo(headCenter.x - w * 0.11f, baseY)
        lineTo(headCenter.x - w * 0.13f, baseY - w * 0.10f)
        lineTo(headCenter.x - w * 0.05f, baseY - w * 0.05f)
        lineTo(headCenter.x, baseY - w * 0.13f)
        lineTo(headCenter.x + w * 0.05f, baseY - w * 0.05f)
        lineTo(headCenter.x + w * 0.13f, baseY - w * 0.10f)
        lineTo(headCenter.x + w * 0.11f, baseY)
        close()
    }
    drawPath(crownPath, Color(0xFFFFB703))
    drawPath(crownPath, Color(0xFFE69F00), style = Stroke(width = w * 0.012f, join = StrokeJoin.Round))
    drawCircle(Color(0xFFE55D7B), radius = w * 0.018f, center = Offset(headCenter.x, baseY - w * 0.04f))
}

private fun DrawScope.drawSailorCap(headCenter: Offset, headSize: Float, w: Float) {
    val baseY = headCenter.y - headSize * 0.36f
    drawArc(
        color = Color.White,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - w * 0.14f, baseY - w * 0.11f),
        size = Size(w * 0.28f, w * 0.18f)
    )
    drawRoundRect(
        color = Color(0xFF1A9385),
        topLeft = Offset(headCenter.x - w * 0.15f, baseY - w * 0.03f),
        size = Size(w * 0.30f, w * 0.05f),
        cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
    )
}

private fun DrawScope.drawBeanie(headCenter: Offset, headSize: Float, w: Float) {
    val baseY = headCenter.y - headSize * 0.34f
    drawArc(
        color = Color(0xFFE55D7B),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - w * 0.16f, baseY - w * 0.13f),
        size = Size(w * 0.32f, w * 0.22f)
    )
    drawRoundRect(
        color = Color(0xFFFF99AC),
        topLeft = Offset(headCenter.x - w * 0.17f, baseY - w * 0.035f),
        size = Size(w * 0.34f, w * 0.055f),
        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
    )
    drawCircle(Color.White, radius = w * 0.038f, center = Offset(headCenter.x, baseY - w * 0.135f))
}

private fun DrawScope.drawSprout(headCenter: Offset, headSize: Float, w: Float) {
    val baseY = headCenter.y - headSize * 0.45f
    drawLine(
        color = Color(0xFF2E7D32),
        start = Offset(headCenter.x, baseY),
        end = Offset(headCenter.x, baseY - w * 0.09f),
        strokeWidth = w * 0.022f,
        cap = StrokeCap.Round
    )
    drawOval(
        color = Color(0xFF66BB6A),
        topLeft = Offset(headCenter.x - w * 0.09f, baseY - w * 0.13f),
        size = Size(w * 0.095f, w * 0.055f)
    )
    drawOval(
        color = Color(0xFF81C784),
        topLeft = Offset(headCenter.x, baseY - w * 0.13f),
        size = Size(w * 0.095f, w * 0.055f)
    )
}

private fun DrawScope.drawHalo(headCenter: Offset, headSize: Float, w: Float) {
    drawOval(
        color = Color(0xFFFFE082),
        topLeft = Offset(headCenter.x - w * 0.14f, headCenter.y - headSize * 0.72f),
        size = Size(w * 0.28f, w * 0.075f),
        style = Stroke(width = w * 0.024f)
    )
}

private fun DrawScope.drawDuckWing(
    wingStyle: String,
    bodyColor: Color,
    w: Float,
    h: Float
) {
    when (wingStyle) {
        "flappy" -> {
            val wingPath = Path().apply {
                moveTo(w * 0.27f, h * 0.56f)
                quadraticTo(w * 0.44f, h * 0.48f, w * 0.57f, h * 0.58f)
                quadraticTo(w * 0.45f, h * 0.72f, w * 0.27f, h * 0.69f)
                close()
            }
            drawPath(wingPath, color = lighten(bodyColor, 0.22f))
            drawPath(
                wingPath,
                color = darken(bodyColor, 0.18f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        "angel" -> {
            val featherOuter = Path().apply {
                moveTo(w * 0.25f, h * 0.62f)
                quadraticTo(w * 0.20f, h * 0.44f, w * 0.40f, h * 0.46f)
                quadraticTo(w * 0.50f, h * 0.52f, w * 0.54f, h * 0.64f)
                quadraticTo(w * 0.42f, h * 0.74f, w * 0.25f, h * 0.62f)
                close()
            }
            drawPath(featherOuter, color = Color(0xFFFFFBEB))
            drawPath(
                featherOuter,
                color = Color(0xFFFDE68A),
                style = Stroke(width = w * 0.020f, join = StrokeJoin.Round)
            )
            drawArc(
                color = Color(0xFFF59E0B).copy(alpha = 0.55f),
                startAngle = 210f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.50f),
                size = Size(w * 0.22f, h * 0.14f),
                style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
            )
        }

        else -> {
            drawOval(
                color = lighten(bodyColor, 0.18f),
                topLeft = Offset(w * 0.28f, h * 0.56f),
                size = Size(w * 0.30f, h * 0.20f)
            )
            drawArc(
                color = darken(bodyColor, 0.16f),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.56f),
                size = Size(w * 0.30f, h * 0.20f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawDuckBill(
    billStyle: String,
    billColor: Color,
    w: Float,
    h: Float
) {
    when (billStyle) {
        "wide" -> {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(lighten(billColor, 0.16f), billColor, darken(billColor, 0.12f))
                ),
                topLeft = Offset(w * 0.63f, h * 0.355f),
                size = Size(w * 0.24f, h * 0.135f)
            )
            drawArc(
                color = darken(billColor, 0.32f),
                startAngle = 12f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.67f, h * 0.39f),
                size = Size(w * 0.16f, h * 0.06f),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )
        }

        "chirp" -> {
            val upper = Path().apply {
                moveTo(w * 0.66f, h * 0.405f)
                quadraticTo(w * 0.78f, h * 0.34f, w * 0.86f, h * 0.38f)
                quadraticTo(w * 0.77f, h * 0.43f, w * 0.66f, h * 0.425f)
                close()
            }
            val lower = Path().apply {
                moveTo(w * 0.66f, h * 0.425f)
                quadraticTo(w * 0.76f, h * 0.44f, w * 0.83f, h * 0.46f)
                quadraticTo(w * 0.73f, h * 0.495f, w * 0.66f, h * 0.445f)
                close()
            }
            drawPath(upper, color = lighten(billColor, 0.08f))
            drawPath(lower, color = darken(billColor, 0.12f))
        }

        else -> {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(lighten(billColor, 0.12f), billColor, darken(billColor, 0.12f))
                ),
                topLeft = Offset(w * 0.65f, h * 0.37f),
                size = Size(w * 0.20f, h * 0.105f)
            )
            drawLine(
                color = darken(billColor, 0.32f),
                start = Offset(w * 0.69f, h * 0.425f),
                end = Offset(w * 0.82f, h * 0.42f),
                strokeWidth = w * 0.015f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawDuckEye(
    eyeStyle: String,
    eyeCenter: Offset,
    w: Float
) {
    when (eyeStyle) {
        "wink" -> {
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(eyeCenter.x - w * 0.040f, eyeCenter.y - w * 0.025f),
                size = Size(w * 0.080f, w * 0.055f),
                style = Stroke(width = w * 0.024f, cap = StrokeCap.Round)
            )
        }

        "starry" -> {
            drawCircle(color = Color(0xFF1E293B), radius = w * 0.042f, center = eyeCenter)
            drawCircle(
                color = Color(0xFFFFE082),
                radius = w * 0.017f,
                center = Offset(eyeCenter.x - w * 0.011f, eyeCenter.y - w * 0.012f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.009f,
                center = Offset(eyeCenter.x + w * 0.013f, eyeCenter.y + w * 0.012f)
            )
        }

        else -> {
            drawCircle(color = Color(0xFF1E293B), radius = w * 0.036f, center = eyeCenter)
            drawCircle(
                color = Color.White,
                radius = w * 0.014f,
                center = Offset(eyeCenter.x - w * 0.01f, eyeCenter.y - w * 0.012f)
            )
        }
    }
}

private fun lighten(color: Color, factor: Float): Color = Color(
    red = (color.red + (1f - color.red) * factor).coerceIn(0f, 1f),
    green = (color.green + (1f - color.green) * factor).coerceIn(0f, 1f),
    blue = (color.blue + (1f - color.blue) * factor).coerceIn(0f, 1f),
    alpha = color.alpha
)

private fun darken(color: Color, factor: Float): Color = Color(
    red = (color.red * (1f - factor)).coerceIn(0f, 1f),
    green = (color.green * (1f - factor)).coerceIn(0f, 1f),
    blue = (color.blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = color.alpha
)
