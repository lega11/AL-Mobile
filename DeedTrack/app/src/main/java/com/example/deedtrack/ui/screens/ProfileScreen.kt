package com.example.deedtrack.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.UserProfile
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText

@Composable
fun ProfileScreen(
    user: UserProfile?,
    onSave: (UserProfile) -> Unit,
    onLogOut: () -> Unit,
    onTab: (MainTab) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var name by remember(user) { mutableStateOf(user?.fullName.orEmpty()) }
    var dob by remember(user) { mutableStateOf(user?.dateOfBirth.orEmpty()) }
    var phone by remember(user) { mutableStateOf(user?.phone.orEmpty()) }
    var address by remember(user) { mutableStateOf(user?.address.orEmpty()) }

    Scaffold(
        topBar = {
            AppTopBar("Profile", showBrand = true, actions = {
                TextButton(onClick = {
                    if (editing && user != null) {
                        onSave(user.copy(fullName = name.trim(), dateOfBirth = dob.trim(), phone = phone.trim(), address = address.trim()))
                    }
                    editing = !editing
                }) { Text(if (editing) "Save" else "Edit") }
            })
        },
        bottomBar = { MainBottomBar(MainTab.PROFILE, onTab) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(8.dp))
                Avatar(user?.initials ?: "?", 72.dp)
                Spacer(Modifier.height(10.dp))
                Text(user?.fullName.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Borrower account", style = MaterialTheme.typography.bodySmall, color = MutedText)
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("Personal details")
            Spacer(Modifier.height(8.dp))

            if (editing) {
                FormField("Full name", name, { name = it })
                Spacer(Modifier.height(10.dp))
                FormField("Date of birth", dob, { dob = it }, placeholder = "e.g. 14 September 1990")
                Spacer(Modifier.height(10.dp))
                FormField("Phone", phone, { phone = it })
                Spacer(Modifier.height(10.dp))
                FormField("Address", address, { address = it })
            } else {
                DetailRow("Full name", user?.fullName)
                DetailRow("Email", user?.email)
                DetailRow("Date of birth", user?.dateOfBirth)
                DetailRow("Phone", user?.phone)
                DetailRow("Address", user?.address)
            }

            Spacer(Modifier.height(20.dp))
            AppCard {
                SettingsRow(Icons.Outlined.Info, "Privacy notice") { showPrivacy = true }
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(Icons.AutoMirrored.Filled.Logout, "Log out", onLogOut)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Agentic Lender · Version 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MutedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPrivacy) PrivacyNoticeDialog { showPrivacy = false }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = MutedText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(16.dp))
        Text(
            value?.ifBlank { "—" } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = CardBorder)
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedText)
    }
}
