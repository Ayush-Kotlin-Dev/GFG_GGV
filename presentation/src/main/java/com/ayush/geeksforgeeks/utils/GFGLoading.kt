package com.ayush.geeksforgeeks.utils

import androidx.compose.runtime.Composable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.R
@Composable
fun GFGLoading() {
    // Add a Box that covers the full screen and blocks interactions
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false, onClick = {}) // Blocks touches but doesn't handle them
        ,
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent background scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )

        Image(
            painter = painterResource(id = R.drawable.geeksforgeeks_logo),
            contentDescription = "Loading",
            modifier = Modifier
                .size(100.dp)
                .alpha(alpha)
        )
    }
}