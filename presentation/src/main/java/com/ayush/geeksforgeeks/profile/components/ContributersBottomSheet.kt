package com.ayush.geeksforgeeks.profile.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.ayush.data.model.ContributorData
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary

@Composable
fun ContributorsContent(
    contributors: List<ContributorData>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp)
    ) {
        Column {
            Text(
                "Contributors",
                style = MaterialTheme.typography.headlineSmall,
                color = GFGPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Meet our amazing contributors who have helped build and improve this app.",
                style = MaterialTheme.typography.bodyLarge,
                color = GFGTextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = contributors,
                key = { it.githubUrl }
            ) { contributor ->
                ContributorSection(
                    contributor = contributor,
                    onError = { error ->
                        Log.e("ContributorsContent", "Error: $error")
                    }
                )
            }
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close", color = GFGPrimary)
        }
    }
}

@Composable
fun ContributorSection(
    contributor: ContributorData,
    onError: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isImageLoading by remember { mutableStateOf(true) }
    var imageLoadError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(1.dp, GFGPrimary, CircleShape)
        ) {
            if (isImageLoading && !imageLoadError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = GFGPrimary
                )
            }

            Image(
                painter = rememberAsyncImagePainter(
                    model = contributor.imageUrl,
                    onState = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Loading -> {
                                isImageLoading = true
                                imageLoadError = false
                            }
                            is AsyncImagePainter.State.Success -> {
                                isImageLoading = false
                                imageLoadError = false
                            }
                            is AsyncImagePainter.State.Error -> {
                                isImageLoading = false
                                imageLoadError = true
                                onError("Failed to load image for ${contributor.name}")
                            }
                            else -> {}
                        }
                    }
                ),
                contentDescription = "${contributor.name}'s profile picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contributor.name,
                style = MaterialTheme.typography.titleMedium,
                color = GFGPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = contributor.role,
                style = MaterialTheme.typography.bodyMedium,
                color = GFGTextPrimary
            )
        }

        IconButton(
            onClick = {
                try {
                    uriHandler.openUri(contributor.githubUrl)
                } catch (e: Exception) {
                    onError("Failed to open GitHub profile: ${e.message}")
                }
            },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = GFGPrimary,
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.github_mark),
                contentDescription = "GitHub profile",
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp)
            )
        }
    }
}