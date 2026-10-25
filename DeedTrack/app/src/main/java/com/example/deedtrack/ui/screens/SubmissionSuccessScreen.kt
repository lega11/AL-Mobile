package com.example.deedtrack.ui.screens

import com.example.deedtrack.ui.theme.SurfaceColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.NavyLight
import com.example.deedtrack.ui.theme.SoftFill

@Composable
fun SubmissionSuccessScreen(
    application: MortgageApplication?,
    onUploadDocuments: () -> Unit,
    onBackToApplications: () -> Unit
) {
    Scaffold(containerColor = SurfaceColor) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) { BrandRow() }
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(NavyLight).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Success")
            Spacer(Modifier.height(6.dp))
            Text("Application submitted", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "We've received your application and will keep you updated at each stage.",
                style = MaterialTheme.typography.bodyMedium, color = MutedText, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            AppCard {
                SectionLabel("Reference number")
                Spacer(Modifier.height(4.dp))
                Text(application?.reference ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status", color = MutedText, modifier = Modifier.weight(1f))
                    application?.let { StatusChip(it.status) }
                }
            }
            Spacer(Modifier.height(12.dp))
            AppCard(background = SoftFill) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Description, background = SurfaceColor)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Next: add your documents", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Uploading them now can help us review your application sooner.",
                            style = MaterialTheme.typography.bodySmall, color = MutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Upload documents", onUploadDocuments)
            TextButton(onClick = onBackToApplications) { Text("Back to applications") }
        }
    }
}
