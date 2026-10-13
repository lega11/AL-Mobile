package com.example.deedtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SoftFill

// ---------- Branding ----------

@Composable
fun BrandMark(size: Dp = 32.dp) {
    Box(
        Modifier.size(size).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.BarChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun BrandRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Text("Agentic Lender", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Avatar(initials: String, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

// ---------- Top & bottom bars ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    showBrand: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        CenterAlignedTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else if (showBrand) {
                    Box(Modifier.padding(start = 16.dp)) { BrandMark(28.dp) }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
        )
        HorizontalDivider(color = CardBorder)
    }
}

enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    DOCUMENTS("Documents", Icons.Outlined.Description),
    PROFILE("Profile", Icons.Outlined.Person)
}

@Composable
fun MainBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}

// ---------- Text helpers ----------

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 1.sp,
        color = MutedText,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun ScreenHeading(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MutedText)
    }
}

// ---------- Cards ----------

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, CardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun IconTile(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary, background: Color = SoftFill) {
    Box(
        Modifier.size(40.dp).background(background, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun InfoNote(icon: ImageVector, text: String) {
    Row(
        Modifier.fillMaxWidth().background(SoftFill, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MutedText)
    }
}

// ---------- Status ----------

private fun statusColors(status: ApplicationStatus): Pair<Color, Color> = when (status) {
    ApplicationStatus.APPLIED -> Color(0xFFEFF1F5) to Color(0xFF374151)
    ApplicationStatus.AI_DELIBERATED -> Color(0xFFE3ECFF) to Color(0xFF1E4FC2)
    ApplicationStatus.UNDERWRITER_APPROVED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    ApplicationStatus.FUNDED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
    ApplicationStatus.CLOSED -> Color(0xFFEFF1F5) to Color(0xFF6B7280)
}

@Composable
fun StatusChip(status: ApplicationStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(status)
    Text(
        status.label,
        modifier = modifier.background(bg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

// ---------- Form fields ----------

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    prefix: String? = null,
    suffix: String? = null,
    isError: Boolean = false
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MutedText) },
            singleLine = true,
            isError = isError,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            prefix = if (prefix != null) { { Text(prefix) } } else null,
            suffix = if (suffix != null) { { Text(suffix, color = MutedText) } } else null,
            shape = RoundedCornerShape(10.dp)
        )
    }
}

/** Number field with a € suffix, like the wireframe. */
@Composable
fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    FormField(
        label = label,
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }) },
        modifier = modifier,
        placeholder = "0",
        keyboardType = KeyboardType.Number,
        suffix = "€",
        isError = isError
    )
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select",
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                isError = isError,
                placeholder = { Text(placeholder, color = MutedText) },
                trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            // Transparent layer so tapping anywhere on the field opens the menu
            Box(Modifier.matchParentSize().clickable { expanded = true })
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

// ---------- Buttons ----------

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.height(50.dp), enabled = enabled, shape = RoundedCornerShape(25.dp)) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(25.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Stepper for the 4-step application form ----------

@Composable
fun FormStepper(current: Int, labels: List<String>) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val reached = index <= current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (reached) MaterialTheme.colorScheme.primary else Color.White)
                        .border(1.5.dp, if (reached) MaterialTheme.colorScheme.primary else CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (index < current) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "${index + 1}",
                            color = if (reached) Color.White else MutedText,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index == current) MaterialTheme.colorScheme.primary else MutedText
                )
            }
        }
    }
}

// ---------- Dialogs ----------

@Composable
fun PrivacyNoticeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Privacy notice") },
        text = {
            Text(
                "Agentic Lender uses your personal, financial and property details only to assess your " +
                    "mortgage application. Your information is shared with your mortgage broker and the bank's " +
                    "underwriting team, kept secure, and you can ask for it to be deleted at any time under GDPR."
            )
        }
    )
}
