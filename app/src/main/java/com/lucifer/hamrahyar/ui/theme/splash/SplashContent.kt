package com.lucifer.hamrahyar.ui.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucifer.hamrahyar.R
import com.lucifer.hamrahyar.ui.theme.Lalezar
import com.lucifer.hamrahyar.ui.theme.Vazir
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashContent(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "techRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- Orbiting Tech Icons ---
        if (progress > 0.15f) {
            val techAlpha = ((progress - 0.15f) * 2f).coerceIn(0f, 1f)
            OrbitingIcon(Icons.Rounded.Smartphone, rotation, 110f, techAlpha)
            OrbitingIcon(Icons.Rounded.Computer, rotation + 120f, 110f, techAlpha)
            OrbitingIcon(Icons.Rounded.Language, rotation + 240f, 110f, techAlpha)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo ---
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        alpha = if (progress < 0.1f) 0f else ((progress - 0.1f) * 3f).coerceIn(0f, 1f)
                        scaleX = if (progress < 0.1f) 0.5f else 0.5f + (progress - 0.1f) * 0.5f
                        scaleY = if (progress < 0.1f) 0.5f else 0.5f + (progress - 0.1f) * 0.5f
                    }
            ) {
                // Glow
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF6C5CE7).copy(alpha = 0.4f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(130.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "همراه یار",
                fontFamily = Lalezar,
                fontSize = 52.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (progress < 0.4f) 0f else ((progress - 0.4f) * 3f).coerceIn(0f, 1f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "دستیار هوشمند خدمات آنلاین شما",
                fontFamily = Vazir,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (progress < 0.65f) 0f else ((progress - 0.65f) * 3f).coerceIn(0f, 1f))
            )
        }
    }
}

@Composable
fun OrbitingIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, angleDegrees: Float, radius: Float, alpha: Float) {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val x = (radius * cos(angleRad)).toFloat()
    val y = (radius * sin(angleRad)).toFloat()

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color(0xFFa29bfe).copy(alpha = alpha * 0.5f),
        modifier = Modifier
            .offset(x = x.dp, y = y.dp)
            .size(36.dp)
    )
}
