package com.ayush.geeksforgeeks.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayush.data.model.Team
import com.ayush.data.model.TeamMember
import com.ayush.data.repository.AuthRepository
import com.ayush.geeksforgeeks.auth.TeamSection
import com.ayush.geeksforgeeks.ui.theme.GFGBlack
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPending
import com.ayush.geeksforgeeks.ui.theme.GFGStatusPendingText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDropdown(
    teams: List<Team>,
    selectedTeam: Team?,
    onTeamSelect: (Team) -> Unit,
    teamSections: List<TeamSection>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedTeam?.name ?: "Select Team",
            onValueChange = {},
            readOnly = true,
            label = { Text("Team") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GFGStatusPendingText,
                unfocusedBorderColor = GFGBlack.copy(alpha = 0.5f),
                focusedLabelColor = GFGStatusPendingText,
                unfocusedLabelColor = GFGBlack,
                focusedTextColor = GFGBlack,
                unfocusedTextColor = GFGBlack
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize()
                .background(GFGStatusPending)
                .alpha(0.8f)
        ) {
            teamSections.forEach { section ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = section.title,
                            fontWeight = FontWeight.Bold,
                            color = GFGStatusPendingText,
                            fontSize = 16.sp
                        )
                    },
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.background(GFGStatusPending.copy(alpha = 0.3f))
                )

                teams.filter { it.id.toInt() in section.idRange }
                    .forEach { team ->
                        DropdownMenuItem(
                            text = { Text(team.name, color = GFGBlack) },
                            onClick = {
                                onTeamSelect(team)
                                expanded = false
                            }
                        )
                    }

                HorizontalDivider(
                    color = GFGStatusPendingText,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDropdown(
    teamMembers: List<TeamMember>,
    selectedMember: TeamMember?,
    onMemberSelect: (TeamMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedMember?.name ?: "Select Your Name",
            onValueChange = {},
            readOnly = true,
            label = { Text("Your Name") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GFGStatusPendingText,
                unfocusedBorderColor = GFGBlack.copy(alpha = 0.5f),
                focusedLabelColor = GFGStatusPendingText,
                unfocusedLabelColor = GFGBlack,
                focusedTextColor = GFGBlack,
                unfocusedTextColor = GFGBlack
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .exposedDropdownSize()
                .background(GFGStatusPending)
                .alpha(0.8f)
        ) {
            teamMembers.forEach { member ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${member.name} (${member.role})",
                            color = GFGBlack
                        )
                    },
                    onClick = {
                        onMemberSelect(member)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = GFGBlack,
                        leadingIconColor = GFGBlack,
                        trailingIconColor = GFGBlack,
                        disabledTextColor = GFGBlack.copy(alpha = 0.5f),
                        disabledLeadingIconColor = GFGBlack.copy(alpha = 0.5f),
                        disabledTrailingIconColor = GFGBlack.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}