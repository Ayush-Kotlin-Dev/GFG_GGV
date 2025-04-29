package com.ayush.geeksforgeeks.presentation.mentorship.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerThreadItem() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Thread header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture shimmer
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Thread title shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Tags shimmer
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Thread message shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer shimmer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}