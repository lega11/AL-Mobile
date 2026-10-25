package com.example.deedtrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.UserProfile
import com.example.deedtrack.data.toAmount
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.euro
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SoftFill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserProfile?,
    applications: List<MortgageApplication>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onNewApplication: () -> Unit,
    onOpenApplication: (String) -> Unit,
    onTab: (MainTab) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar("My applications", showBrand = true, actions = {
                Box(Modifier.padding(end = 16.dp)) { Avatar(user?.initials ?: "?", 32.dp) }
            })
        },
        bottomBar = { MainBottomBar(MainTab.HOME, onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New application") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onNewApplication,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
      PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(padding)
      ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionLabel("Welcome back${user?.firstName?.let { ", $it" } ?: ""}")
                Spacer(Modifier.height(4.dp))
                Text("Your mortgage journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            items(applications, key = { it.id }) { app ->
                ApplicationCard(app, onClick = { onOpenApplication(app.id) })
            }
            if (applications.isEmpty()) {
                item { EmptyApplicationsCard() }
            }
            item { Spacer(Modifier.height(72.dp)) } // room for the button
        }
      }
    }
}

@Composable
private fun ApplicationCard(app: MortgageApplication, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(Icons.Outlined.Home)
            Spacer(Modifier.weight(1f))
            StatusChip(app.status)
        }
        Spacer(Modifier.height(12.dp))
        Text(app.addressLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (app.areaLine.isNotBlank()) {
            Text(app.areaLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Loan amount", style = MaterialTheme.typography.bodySmall, color = MutedText)
            Spacer(Modifier.weight(1f))
            Text(euro(app.details.loanAmount.toAmount()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (app.status.ordinal + 1f) / ApplicationStatus.entries.size },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = CardBorder
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("View application", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedText)
        }
    }
}

@Composable
private fun EmptyApplicationsCard() {
    AppCard(background = SoftFill) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Filled.Add, background = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.width(12.dp))
            Column {
                SectionLabel("No applications yet")
                Text("Tap New application to start your mortgage application.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
