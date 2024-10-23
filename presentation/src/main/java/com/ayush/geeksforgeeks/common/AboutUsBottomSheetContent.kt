package com.ayush.geeksforgeeks.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary


@Composable
fun AboutUsContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())  // Make the content scrollable
    ) {
        Text(
            "About Us",
            style = MaterialTheme.typography.headlineSmall,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "GeeksforGeeks Student Chapter GGV is a university-based community " +
                    "that aims to promote technology learning, coding skills, and " +
                    "career development among students. We organize workshops, coding " +
                    "competitions, and provide resources to help students excel in " +
                    "their tech careers.",
            style = MaterialTheme.typography.bodyLarge,
            color = GFGTextPrimary
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Teams Section
        Text(
            "Our Teams",
            style = MaterialTheme.typography.titleLarge,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        TeamSection(
            "Technical Team",
            "Responsible for organizing coding workshops, hackathons, and maintaining our technical projects."
        )
        TeamSection(
            "Content Team",
            "Creates and curates high-quality educational content for our blog, social media, and workshops."
        )
        TeamSection(
            "Design Team",
            "Handles all visual aspects of our chapter, including graphics, UI/UX for our projects, and event posters."
        )
        TeamSection(
            "Event Management Team",
            "Plans and executes our events, ensuring smooth operation and participant engagement."
        )
        TeamSection(
            "Public Relations Team",
            "Manages our social media presence, liaises with other organizations, and promotes our events."
        )

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = {
                onClose()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close", color = GFGPrimary)
        }
    }
}

@Composable
fun TeamSection(teamName: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = teamName,
            style = MaterialTheme.typography.titleMedium,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = GFGTextPrimary
        )
    }
}