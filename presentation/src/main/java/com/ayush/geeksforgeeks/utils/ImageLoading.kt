package com.ayush.geeksforgeeks.utils
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary

@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
        ),
        label = "progress animation"
    )

    Box(
        modifier = modifier
            .size(60.dp)
            .graphicsLayer {
                scaleX = progress
                scaleY = progress
                alpha = 1f - progress
            }
            .border(
                width = 5.dp,
                color = color,
                shape = CircleShape
            )
    )
}
private const val PADDING_PERCENTAGE_OUTER_CIRCLE = 0.15f
private const val PADDING_PERCENTAGE_INNER_CIRCLE = 0.3f
private const val POSITION_START_OFFSET_OUTER_CIRCLE = 90f
private const val POSITION_START_OFFSET_INNER_CIRCLE = 135f
@Composable
fun TripleOrbitLoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = GFGPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
        ),
        label = "rotation animation"
    )
    var width by remember {
        mutableIntStateOf(0)
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .onSizeChanged {
                width = it.width
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = color,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
        )
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = color.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    with(LocalDensity.current) {
                        (width * PADDING_PERCENTAGE_INNER_CIRCLE).toDp()
                    }
                )
                .graphicsLayer {
                    rotationZ = rotation + POSITION_START_OFFSET_INNER_CIRCLE
                }
        )
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = color.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    with(LocalDensity.current) {
                        (width * PADDING_PERCENTAGE_OUTER_CIRCLE).toDp()
                    }
                )
                .graphicsLayer {
                    rotationZ = rotation + POSITION_START_OFFSET_OUTER_CIRCLE
                }
        )
    }
}