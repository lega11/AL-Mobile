package com.example.deedtrack.ui.screens

import com.example.deedtrack.ui.theme.ErrorBg
import com.example.deedtrack.ui.theme.SurfaceColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.data.Decision
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.toAmount
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.euro
import com.example.deedtrack.ui.formatDate
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.ErrorRed
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SuccessBg
import com.example.deedtrack.ui.theme.SuccessGreen

/** Application progress tracking: status timeline, decision and blockchain record. */
@Composable
fun ApplicationDetailScreen(
    application: MortgageApplication?,
    onBack: () -> Unit,
    onDocuments: () -> Unit,
    onSimulateNextStatus: () -> Unit
) {
    Scaffold(topBar = { AppTopBar("Application", onBack = onBack) }) { padding ->
        if (application == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Application not found", color = MutedText)
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            AppCard {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(application.addressLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(application.areaLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                    StatusChip(application.status)
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Loan amount", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Text(euro(application.details.loanAmount.toAmount()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Reference", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Text(application.reference, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Application progress")
            Spacer(Modifier.height(12.dp))
            StatusTimeline(application)

            application.decision?.let { decision ->
                Spacer(Modifier.height(16.dp))
                val good = decision != Decision.DECLINED
                AppCard(background = if (good) SuccessBg else ErrorBg) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = if (good) SuccessGreen else ErrorRed)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            SectionLabel("Decision")
                            Text(decision.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            application.blockchainTxRef?.let { tx ->
                Spacer(Modifier.height(12.dp))
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Link)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Recorded on blockchain", fontWeight = FontWeight.SemiBold)
                            Text("Transaction ref: $tx", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SecondaryButton("View documents (${application.documentsDone}/${DocumentType.entries.size})", onDocuments)

            if (application.status != ApplicationStatus.CLOSED) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSimulateNextStatus, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Simulate next status (prototype demo)", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusTimeline(application: MortgageApplication) {
    val reachedAt = application.history.associate { it.status to it.timestamp }
    val statuses = ApplicationStatus.entries
    Column {
        statuses.forEachIndexed { index, status ->
            val reached = reachedAt.containsKey(status)
            val current = status == application.status
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (reached) MaterialTheme.colorScheme.primary else SurfaceColor)
                            .border(2.dp, if (reached) MaterialTheme.colorScheme.primary else CardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (reached) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                    if (index < statuses.lastIndex) {
                        Box(
                            Modifier.width(2.dp).height(40.dp)
                                .background(if (reachedAt.containsKey(statuses[index + 1])) MaterialTheme.colorScheme.primary else CardBorder)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(top = 2.dp)) {
                    Text(
                        status.label,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        color = if (reached) MaterialTheme.colorScheme.onSurface else MutedText
                    )
                    Text(
                        reachedAt[status]?.let { formatDate(it) } ?: if (current) status.description else "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText
                    )
                    if (current) {
                        Text(status.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
