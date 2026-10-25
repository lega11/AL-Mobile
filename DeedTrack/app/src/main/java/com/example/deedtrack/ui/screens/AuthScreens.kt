package com.example.deedtrack.ui.screens

import com.example.deedtrack.ui.theme.SurfaceColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.UserProfile
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SuccessBg
import com.example.deedtrack.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

// ======================= Log in =======================

@Composable
fun LoginScreen(
    onLogIn: suspend (email: String, password: String) -> String?,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = SurfaceColor) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            BrandRow()
            Spacer(Modifier.height(40.dp))
            ScreenHeading("Welcome back", "Log in to manage your mortgage application.")
            Spacer(Modifier.height(24.dp))

            FormField(
                "Email address", email, { email = it; errorMessage = null },
                placeholder = "name@example.ie", keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(16.dp))
            FormField(
                "Password", password, { password = it; errorMessage = null },
                placeholder = "Enter your password", isPassword = true
            )
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?", textDecoration = TextDecoration.Underline)
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            PrimaryButton(
                if (loading) "Logging in…" else "Log in",
                {
                    loading = true
                    scope.launch {
                        errorMessage = onLogIn(email.trim(), password)
                        loading = false
                    }
                },
                enabled = !loading
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("New to Agentic Lender?", color = MutedText, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onCreateAccount) { Text("Create account") }
            }

            Spacer(Modifier.height(24.dp))
            InfoNote(Icons.Filled.Lock, "Your account is protected with bank-grade encryption.")
        }
    }
}

// ======================= Forgot password =======================

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSendReset: suspend (email: String) -> String?
) {
    var email by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = SurfaceColor, topBar = { AppTopBar("Forgot password?", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Box(
                Modifier.size(56.dp).border(1.dp, CardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(20.dp))
            ScreenHeading(
                "Reset password",
                "Enter your email and we'll send you a secure link to choose a new password."
            )
            Spacer(Modifier.height(24.dp))
            FormField("Email address", email, { email = it }, placeholder = "name@example.ie", keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(20.dp))
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            PrimaryButton(
                if (sending) "Sending…" else "Send reset link",
                {
                    sending = true
                    scope.launch {
                        errorMessage = onSendReset(email.trim())
                        sent = errorMessage == null
                        sending = false
                    }
                },
                enabled = email.contains("@") && !sending
            )

            if (sent) {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .background(SuccessBg, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Reset link sent", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Check your inbox. The link expires in 30 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Back to log in", textDecoration = TextDecoration.Underline)
            }
        }
    }
}

// ======================= Create account =======================

@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onCreate: suspend (UserProfile, String) -> String?
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val problems = buildList {
        if (fullName.isBlank()) add("Enter your full name.")
        if (!email.contains("@")) add("Enter a valid email address.")
        if (password.length < 8) add("Password must be at least 8 characters.")
        if (confirm != password) add("Passwords don't match.")
        if (!consent) add("Tick the box to agree to the privacy notice.")
    }

    Scaffold(containerColor = SurfaceColor, topBar = { AppTopBar("Create account", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            ScreenHeading("Let's get you started", "Create your account to save your application.")
            Spacer(Modifier.height(20.dp))
            FormField("Full name", fullName, { fullName = it }, placeholder = "Enter your full name",
                isError = showErrors && fullName.isBlank())
            Spacer(Modifier.height(12.dp))
            FormField("Email address", email, { email = it }, placeholder = "name@example.ie",
                keyboardType = KeyboardType.Email, isError = showErrors && !email.contains("@"))
            Spacer(Modifier.height(12.dp))
            FormField("Phone number", phone, { phone = it }, prefix = "+353 ", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(12.dp))
            FormField("Password", password, { password = it }, placeholder = "At least 8 characters",
                isPassword = true, isError = showErrors && password.length < 8)
            Spacer(Modifier.height(12.dp))
            FormField("Confirm password", confirm, { confirm = it }, placeholder = "Re-enter password",
                isPassword = true, isError = showErrors && confirm != password)

            TextButton(onClick = { showPrivacy = true }, contentPadding = PaddingValues(0.dp)) {
                Text("Read our privacy notice", textDecoration = TextDecoration.Underline)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text(
                    "I agree to the processing of my data for my mortgage application.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showErrors && problems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                problems.forEach {
                    Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            serverError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                if (creating) "Creating account…" else "Create account",
                {
                    if (problems.isEmpty()) {
                        val fullPhone = if (phone.isBlank()) "" else "+353 ${phone.trim()}"
                        creating = true
                        serverError = null
                        scope.launch {
                            serverError = onCreate(UserProfile(fullName.trim(), email.trim(), fullPhone), password)
                            creating = false
                        }
                    } else {
                        showErrors = true
                    }
                },
                enabled = !creating
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already registered?", color = MutedText, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onBack) { Text("Log in") }
            }
        }
    }

    if (showPrivacy) PrivacyNoticeDialog { showPrivacy = false }
}
