package com.example.deedtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.toAmount
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.euro
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText

@Composable
fun ReviewSubmitScreen(
    form: ApplicationDraft,
    onBack: () -> Unit,
    onEditStep: (Int) -> Unit,
    onSubmit: () -> Unit
) {
    var confirmed by remember { mutableStateOf(false) }

    val sections = listOf(
        "Personal & employment" to listOf(form.employmentStatus, form.employer).filter { it.isNotBlank() }.joinToString(" · "),
        "Financial details" to "Income ${euro(form.grossIncome.toAmount())} · Deposit ${euro(form.deposit.toAmount())}",
        "Property details" to "${form.propertyAddress} · ${euro(form.askingPrice.toAmount())}",
        "Loan request" to "${euro(form.loanAmount.toAmount())} over ${form.termYears} years"
    )

    Scaffold(
        containerColor = Color.White,
        topBar = { AppTopBar("Review & submit", onBack = onBack) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)) {
                PrimaryButton("Submit application", onSubmit, enabled = confirmed)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            ScreenHeading("Check your application", "Please confirm these details are complete and correct.")
            Spacer(Modifier.height(16.dp))
            AppCard {
                sections.forEachIndexed { index, (title, summary) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).border(1.dp, CardBorder, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(summary.ifBlank { "Complete" }, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        TextButton(onClick = { onEditStep(index) }) { Text("Edit") }
                    }
                    if (index < sections.lastIndex) HorizontalDivider(color = CardBorder)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text(
                    "I confirm that the information provided is accurate and understand it is used for assessing my mortgage application.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
