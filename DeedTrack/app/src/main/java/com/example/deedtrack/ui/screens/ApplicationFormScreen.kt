package com.example.deedtrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.MortgageMath
import com.example.deedtrack.data.ValuationService
import com.example.deedtrack.data.toAmount
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.euro
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.NavyLight
import kotlin.math.roundToInt

val STEP_LABELS = listOf("Personal", "Financial", "Property", "Loan")

private val EMPLOYMENT = listOf("Employed full-time", "Employed part-time", "Self-employed", "Contract", "Unemployed", "Retired")
private val PROPERTY_TYPES = listOf("House", "Apartment", "Bungalow", "Duplex", "Townhouse")
private val BER = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "D1", "D2", "E1", "E2", "F", "G", "Exempt")
private val COUNTIES = listOf(
    "Carlow", "Cavan", "Clare", "Cork", "Donegal", "Dublin", "Galway", "Kerry", "Kildare", "Kilkenny", "Laois",
    "Leitrim", "Limerick", "Longford", "Louth", "Mayo", "Meath", "Monaghan", "Offaly", "Roscommon", "Sligo",
    "Tipperary", "Waterford", "Westmeath", "Wexford", "Wicklow"
)

/** Returns true when the step has everything it needs to continue. */
fun isStepComplete(step: Int, d: ApplicationDraft): Boolean = when (step) {
    0 -> d.employmentStatus.isNotBlank() && d.yearsEmployed.isNotBlank() &&
        (d.employer.isNotBlank() || d.employmentStatus in listOf("Unemployed", "Retired"))
    1 -> d.grossIncome.isNotBlank() && d.deposit.isNotBlank()
    2 -> d.propertyAddress.isNotBlank() && d.propertyType.isNotBlank() && d.county.isNotBlank() &&
        d.askingPrice.toAmount() > 0
    3 -> d.loanAmount.toAmount() > 0 && d.loanAmount.toAmount() <= d.askingPrice.toAmount()
    else -> true
}

@Composable
fun ApplicationFormScreen(
    initialStep: Int,
    form: ApplicationDraft,
    valuation: ValuationService,
    onUpdate: ((ApplicationDraft) -> ApplicationDraft) -> Unit,
    onExit: () -> Unit,
    onSaveDraft: () -> Unit,
    onReview: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(initialStep.coerceIn(0, 3)) }
    var showErrors by remember(step) { mutableStateOf(false) }

    fun next() {
        if (!isStepComplete(step, form)) { showErrors = true; return }
        if (step == 3) onReview() else step++
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            AppTopBar("New application", onBack = { if (step == 0) onExit() else step-- }, actions = {
                Text("Draft", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 16.dp))
            })
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step == 0) SecondaryButton("Save draft", onSaveDraft, Modifier.weight(1f))
                else SecondaryButton("Back", { step-- }, Modifier.weight(1f))
                PrimaryButton(if (step == 3) "Review" else "Next", { next() }, Modifier.weight(1f))
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            FormStepper(step, STEP_LABELS)
            Spacer(Modifier.height(20.dp))
            SectionLabel("Step ${step + 1} of 4")
            Spacer(Modifier.height(4.dp))
            when (step) {
                0 -> PersonalStep(form, onUpdate, showErrors)
                1 -> FinancialStep(form, onUpdate, showErrors)
                2 -> PropertyStep(form, onUpdate, showErrors, valuation)
                else -> LoanStep(form, onUpdate, showErrors)
            }
            if (showErrors && !isStepComplete(step, form)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (step == 3) "Enter a loan amount that is not more than the asking price."
                    else "Please fill in the highlighted fields.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private val gap = Modifier.height(12.dp)

@Composable
private fun PersonalStep(d: ApplicationDraft, onUpdate: ((ApplicationDraft) -> ApplicationDraft) -> Unit, errors: Boolean) {
    ScreenHeading("Personal & employment", "Tell us about your current circumstances.")
    Spacer(Modifier.height(16.dp))
    DropdownField("Employment status", d.employmentStatus, EMPLOYMENT, { v -> onUpdate { it.copy(employmentStatus = v) } },
        placeholder = "Select status", isError = errors && d.employmentStatus.isBlank())
    Spacer(gap)
    FormField("Employer", d.employer, { v -> onUpdate { it.copy(employer = v) } }, placeholder = "Employer name",
        isError = errors && d.employer.isBlank() && d.employmentStatus !in listOf("Unemployed", "Retired"))
    Spacer(gap)
    FormField("Years employed", d.yearsEmployed, { v -> onUpdate { it.copy(yearsEmployed = v.filter(Char::isDigit)) } },
        placeholder = "0", keyboardType = KeyboardType.Number, isError = errors && d.yearsEmployed.isBlank())
    Spacer(gap)
    DropdownField("Number of applicants", d.applicants, listOf("1", "2"), { v -> onUpdate { it.copy(applicants = v) } })
    Spacer(gap)
    DropdownField("Number of dependants", d.dependants, listOf("0", "1", "2", "3", "4", "5+"), { v -> onUpdate { it.copy(dependants = v) } })
}

@Composable
private fun FinancialStep(d: ApplicationDraft, onUpdate: ((ApplicationDraft) -> ApplicationDraft) -> Unit, errors: Boolean) {
    ScreenHeading("Financial details", "Use annual gross values and estimates.")
    Spacer(Modifier.height(16.dp))
    MoneyField("Gross annual income", d.grossIncome, { v -> onUpdate { it.copy(grossIncome = v) } }, isError = errors && d.grossIncome.isBlank())
    Spacer(gap)
    MoneyField("Other annual income", d.otherIncome, { v -> onUpdate { it.copy(otherIncome = v) } })
    Spacer(gap)
    MoneyField("Monthly loan repayments", d.monthlyRepayments, { v -> onUpdate { it.copy(monthlyRepayments = v) } })
    Spacer(gap)
    MoneyField("Credit card limits", d.creditCardLimits, { v -> onUpdate { it.copy(creditCardLimits = v) } })
    Spacer(gap)
    MoneyField("Savings / deposit amount", d.deposit, { v -> onUpdate { it.copy(deposit = v) } }, isError = errors && d.deposit.isBlank())
    Spacer(Modifier.height(16.dp))
    InfoNote(Icons.Outlined.Info, "We may ask for supporting documentation.")
}

@Composable
private fun PropertyStep(
    d: ApplicationDraft,
    onUpdate: ((ApplicationDraft) -> ApplicationDraft) -> Unit,
    errors: Boolean,
    valuation: ValuationService
) {
    ScreenHeading("Property details", "Tell us about the home you intend to buy.")
    Spacer(Modifier.height(16.dp))
    FormField("Property address", d.propertyAddress, { v -> onUpdate { it.copy(propertyAddress = v) } },
        placeholder = "e.g. 14 Oakwood Avenue", isError = errors && d.propertyAddress.isBlank())
    Spacer(gap)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField("Property type", d.propertyType, PROPERTY_TYPES, { v -> onUpdate { it.copy(propertyType = v) } },
            Modifier.weight(1f), isError = errors && d.propertyType.isBlank())
        DropdownField("County", d.county, COUNTIES, { v -> onUpdate { it.copy(county = v) } },
            Modifier.weight(1f), isError = errors && d.county.isBlank())
    }
    Spacer(gap)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField("Area", d.area, { v -> onUpdate { it.copy(area = v) } }, Modifier.weight(1f), placeholder = "e.g. Dublin 14")
        DropdownField("BER rating", d.berRating, BER, { v -> onUpdate { it.copy(berRating = v) } }, Modifier.weight(1f))
    }
    Spacer(gap)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownField("Bedrooms", d.bedrooms, (1..6).map { "$it" }, { v -> onUpdate { it.copy(bedrooms = v) } }, Modifier.weight(1f))
        DropdownField("Bathrooms", d.bathrooms, (1..4).map { "$it" }, { v -> onUpdate { it.copy(bathrooms = v) } }, Modifier.weight(1f))
    }
    Spacer(gap)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormField("Floor area", d.floorArea, { v -> onUpdate { it.copy(floorArea = v.filter(Char::isDigit)) } },
            Modifier.weight(1f), placeholder = "0", keyboardType = KeyboardType.Number, suffix = "m²")
        MoneyField("Asking price", d.askingPrice, { v -> onUpdate { it.copy(askingPrice = v) } },
            Modifier.weight(1f), isError = errors && d.askingPrice.toAmount() <= 0)
    }

    val estimate = valuation.estimate(d)
    if (estimate != null) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().background(NavyLight, RoundedCornerShape(12.dp)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(Icons.Outlined.AutoAwesome, background = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                SectionLabel("Estimated property value")
                Text(euro(estimate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text("vs asking price\n${euro(d.askingPrice.toAmount())}", style = MaterialTheme.typography.labelSmall, color = MutedText)
        }
        Text(
            "Prototype estimate – will come from the AL-AI valuation model.",
            style = MaterialTheme.typography.labelSmall, color = MutedText,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun LoanStep(d: ApplicationDraft, onUpdate: ((ApplicationDraft) -> ApplicationDraft) -> Unit, errors: Boolean) {
    val asking = d.askingPrice.toAmount()
    val loan = d.loanAmount.toAmount()

    ScreenHeading("Loan request", "Choose the amount and term that suit you.")
    Spacer(Modifier.height(16.dp))
    MoneyField("Loan amount", d.loanAmount, { v -> onUpdate { it.copy(loanAmount = v) } },
        isError = errors && (loan <= 0 || loan > asking))
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Term in years", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("${d.termYears} years", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
    Slider(
        value = d.termYears.toFloat(),
        onValueChange = { v -> onUpdate { it.copy(termYears = v.roundToInt()) } },
        valueRange = 5f..35f,
        steps = 29
    )
    Row {
        Text("5", style = MaterialTheme.typography.labelSmall, color = MutedText)
        Spacer(Modifier.weight(1f))
        Text("35", style = MaterialTheme.typography.labelSmall, color = MutedText)
    }

    Spacer(Modifier.height(16.dp))
    AppCard {
        SectionLabel("Loan summary")
        Spacer(Modifier.height(10.dp))
        SummaryLine("Your deposit", euro((asking - loan).coerceAtLeast(0.0)))
        SummaryLine("Loan-to-value", "%.1f%%".format(MortgageMath.loanToValue(loan, asking)))
        HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Estimated monthly\nrepayment", style = MaterialTheme.typography.bodyMedium, color = MutedText, modifier = Modifier.weight(1f))
            Text(euro(MortgageMath.monthlyRepayment(loan, d.termYears)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Illustrative only, based on a ${MortgageMath.ILLUSTRATIVE_RATE}% variable rate.",
            style = MaterialTheme.typography.labelSmall, color = MutedText
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedText, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
