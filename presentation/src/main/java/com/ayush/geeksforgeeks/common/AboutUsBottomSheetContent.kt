package com.ayush.geeksforgeeks.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ayush.geeksforgeeks.R
import com.ayush.geeksforgeeks.ui.theme.GFGPrimary
import com.ayush.geeksforgeeks.ui.theme.GFGTextPrimary

@Composable
fun AboutUsContent(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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

        Text(
            "Follow Us",
            style = MaterialTheme.typography.titleLarge,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { uriHandler.openUri("https://www.instagram.com/gfgsc_ggv/") }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.instagram_1_svgrepo_com),
                    contentDescription = "Instagram",
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://x.com/gfgsc_ggv") }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.twitter_color_svgrepo_com),
                    contentDescription = "Twitter",
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://www.linkedin.com/company/gfgsc-ggv/posts/?feedView=all") }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.linkedin_svgrepo_com),
                    contentDescription = "LinkedIn",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Our Core Team",
            style = MaterialTheme.typography.titleLarge,
            color = GFGPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val coreTeamMembers = listOf(
            TeamMember("Anshuman Mishra", "Chairperson", "https://fastly.picsum.photos/id/237/200/301.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/anshuman-mishra-03329925a/"),
            TeamMember("Aditya Raj", "Vice-ChairPerson", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/adityaagupta01/"),
            TeamMember("Piyush keshari", "Event Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/piyush-keshri-ba215026b/"),
            TeamMember("Anuj Vishwakarma", "Technical Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/anuj-vishwakarma1/"),
            TeamMember("Sakshi Agrawal", "Content Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/agrawalsakshi04/"),
            TeamMember("Sameer", "GD/Branding Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/sameer-1b1b3b1b3/"),
            TeamMember("Kishan Sahu", "Social Media Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/kishansahu03/"),
            TeamMember("Chirag shinde", "Marketing and PR Head", "https://fastly.picsum.photos/id/237/200/300.jpg?hmac=TmmQSbShHz9CdQm0NkEjx1Dyh_Y984R9LpNrpvH2D_U" , "https://www.linkedin.com/in/chirag-shinde-1b1b3b1b3/"),
        )

        coreTeamMembers.forEach { member ->
            TeamMemberSection(member)
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
fun TeamMemberSection(member: TeamMember) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = member.imageUrl),
            contentDescription = "${member.name}'s profile picture",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                color = GFGPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodyMedium,
                color = GFGTextPrimary
            )
        }
        IconButton(
            onClick = { uriHandler.openUri(member.linkedInUrl) },
            modifier = Modifier.size(40.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.linkedin_svgrepo_com),
                contentDescription = "LinkedIn profile",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

data class TeamMember(
    val name: String,
    val role: String,
    val imageUrl: String,
    val linkedInUrl: String
)