package com.lucifer.hamrahyar.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val animationController = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Longer duration to show all animations clearly (4 seconds)
        animationController.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4000, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SplashBackground(animationController.value)
        SplashContent(animationController.value)
    }
}
