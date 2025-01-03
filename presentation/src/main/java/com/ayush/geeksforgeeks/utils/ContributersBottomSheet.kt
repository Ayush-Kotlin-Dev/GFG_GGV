package com.ayush.geeksforgeeks.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary
import com.github.theapache64.fig.Fig
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class Contributor(
    val name: String,
    val role: String,
    val imageUrl: String,
    val githubUrl: String
)

@Composable
fun ContributorsContent(onClose: () -> Unit) {
    var contributors by remember { mutableStateOf<List<Contributor>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val sheetUrl = "https://docs.google.com/spreadsheets/d/1yWZdD7-FPbYneSXY5bpatnMkp8LO1RyeCwvZ6JjVWJE/edit?usp=sharing"
//                contributors = fetchContributors(sheetUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
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


        contributors.forEach { contributor ->
            ContributorSection(contributor)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close", color = GFGPrimary)
        }
    }
}

@Composable
fun ContributorSection(contributor: Contributor) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = contributor.imageUrl),
            contentDescription = "${contributor.name}'s profile picture",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )
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
            onClick = { uriHandler.openUri(contributor.githubUrl) },
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


