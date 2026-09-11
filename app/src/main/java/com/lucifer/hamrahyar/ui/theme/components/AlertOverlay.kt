package com.lucifer.hamrahyar.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lucifer.hamrahyar.ui.theme.Vazir

@Composable
fun AlertOverlay(message: String, visible: Boolean) {

    if (!visible) return

    val infinite = rememberInfiniteTransition()
    val alpha by infinite.animateFloat(
        0.6f, 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontFamily = Vazir,
                color = Color.White
            )
        }
    }
}