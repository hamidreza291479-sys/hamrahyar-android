package com.lucifer.hamrahyar.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SplashBackground(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Premium Dark Galaxy Background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F0C29),
                    Color(0xFF302B63),
                    Color(0xFF24243E)
                )
            )
        )

        // Digital Grid (Social Media / Cyber Space vibe)
        val gridStep = 80f
        for (x in 0..(width / gridStep).toInt()) {
            drawLine(
                color = Color(0xFF6C5CE7).copy(alpha = 0.08f * progress),
                start = Offset(x * gridStep, 0f),
                end = Offset(x * gridStep, height),
                strokeWidth = 1f
            )
        }
        for (y in 0..(height / gridStep).toInt()) {
            drawLine(
                color = Color(0xFF6C5CE7).copy(alpha = 0.08f * progress),
                start = Offset(0f, y * gridStep),
                end = Offset(width, y * gridStep),
                strokeWidth = 1f
            )
        }

        // Floating glowing orbs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6C5CE7).copy(alpha = 0.15f * progress), Color.Transparent),
                center = Offset(width * 0.5f, height * 0.4f),
                radius = (width * 0.8f * progress).coerceAtLeast(1f)
            )
        )

        // Animated "Stars" or Dust
        val random = java.util.Random(42)
        repeat(40) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            val radius = random.nextFloat() * 2f
            drawCircle(
                color = Color.White.copy(alpha = starAlpha * random.nextFloat()),
                center = Offset(x, y),
                radius = radius
            )
        }
    }
}
