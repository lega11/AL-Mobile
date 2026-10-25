package com.example.deedtrack

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.data.Decision
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.StatusEvent
import com.example.deedtrack.data.UploadState
import com.example.deedtrack.data.UserProfile
import com.example.deedtrack.ui.screens.ApplicationDetailScreen
import com.example.deedtrack.ui.screens.CreateAccountScreen
import com.example.deedtrack.ui.screens.DocumentChecklistScreen
import com.example.deedtrack.ui.screens.LoginScreen
import com.example.deedtrack.ui.screens.ReviewSubmitScreen
import com.example.deedtrack.ui.theme.DeedTrackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * AL3YGP-57: Compose UI tests for the main borrower flows.
 * Run with an emulator or phone connected: right-click this file → Run.
 */
@RunWith(AndroidJUnit4::class)
class BorrowerFlowsUiTest {

    @get:Rule val rule = createComposeRule()

    private fun field(placeholder: String) = rule.onNode(hasSetTextAction() and hasText(placeholder))
    private fun button(text: String) = rule.onNode(hasText(text) and hasClickAction())

    private fun application(status: ApplicationStatus, decision: Decision? = null, tx: String? = null,
                            documents: Map<DocumentType, List<String>> = emptyMap()): MortgageApplication {
        val history = ApplicationStatus.entries.take(status.ordinal + 1).mapIndexed { i, s -> StatusEvent(s, 1_700_000_000_000L + i * 3_600_000L) }
        return MortgageApplication(
            id = "test-1", reference = "AL-IE-2026-12345", ownerEmail = "test@example.ie", createdAt = 1_700_000_000_000L,
            details = ApplicationDraft(propertyAddress = "14 Oakwood Avenue", county = "Dublin", area = "Dublin 14", loanAmount = "275000"),
            history = history, decision = decision, blockchainTxRef = tx, documents = documents
        )
    }

    // ---------- Log in ----------

    @Test fun logInShowsErrorFromServer() {
        var sentEmail = ""
        rule.setContent {
            DeedTrackTheme {
                LoginScreen(
                    onLogIn = { email, _ -> sentEmail = email; "Incorrect email or password." },
                    onForgotPassword = {}, onCreateAccount = {}
                )
            }
        }
        field("name@example.ie").performTextInput("  test@example.ie ")
        field("Enter your password").performTextInput("wrong-password")
        button("Log in").performClick()
        rule.onNodeWithText("Incorrect email or password.").assertIsDisplayed()
        assertEquals("test@example.ie", sentEmail)
    }

    // ---------- Sign up ----------

    @Test fun signUpValidatesAndNeedsConsent() {
        var created: UserProfile? = null
        rule.setContent {
            DeedTrackTheme { CreateAccountScreen(onBack = {}, onCreate = { profile, _ -> created = profile; null }) }
        }
        button("Create account").performScrollTo().performClick()
        rule.onNodeWithText("• Enter your full name.").performScrollTo().assertIsDisplayed()
        assertEquals(null, created)

        field("Enter your full name").performTextInput("Test Borrower")
        field("name@example.ie").performTextInput("test@example.ie")
        field("At least 8 characters").performTextInput("password123")
        field("Re-enter password").performTextInput("password123")
        button("Create account").performScrollTo().performClick()
        rule.onNodeWithText("• Tick the box to agree to the privacy notice.").performScrollTo().assertIsDisplayed()

        rule.onNode(isToggleable()).performScrollTo().performClick()
        button("Create account").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals("Test Borrower", created?.fullName)
        assertEquals("test@example.ie", created?.email)
    }

    // ---------- Submit application ----------

    @Test fun submitNeedsConfirmationTick() {
        var submitted = false
        rule.setContent {
            DeedTrackTheme {
                ReviewSubmitScreen(
                    form = ApplicationDraft(employmentStatus = "Employed full-time", propertyAddress = "14 Oakwood Avenue",
                        askingPrice = "320000", loanAmount = "275000"),
                    onBack = {}, onEditStep = {}, onSubmit = { submitted = true }
                )
            }
        }
        rule.onNodeWithText("Personal & employment").assertIsDisplayed()
        button("Submit application").assertIsNotEnabled()
        rule.onNode(isToggleable()).performScrollTo().performClick()
        button("Submit application").assertIsEnabled().performClick()
        assertTrue(submitted)
    }

    // ---------- Upload documents ----------

    @Test fun checklistShowsMissingUploadedAndRetry() {
        var retried: String? = null
        val failed = "file:///data/deeds/payslip.jpg"
        rule.setContent {
            DeedTrackTheme {
                DocumentChecklistScreen(
                    application = application(ApplicationStatus.APPLIED, documents = mapOf(
                        DocumentType.PHOTO_ID to listOf("gs://bucket/id.jpg"),
                        DocumentType.PAYSLIPS to listOf(failed)
                    )),
                    uploads = mapOf(failed to UploadState.Failed("No internet connection.")),
                    onAddDocuments = { _, _, _ -> }, onRetry = { _, _, uri -> retried = uri }, onRemove = { _, _, _ -> },
                    onBack = {}
                )
            }
        }
        rule.onNodeWithText("Uploaded · 1 file").assertIsDisplayed()
        rule.onAllNodesWithText("Missing · Required").assertCountEquals(2)
        rule.onNodeWithText("Retry").performScrollTo().performClick()
        assertEquals(failed, retried)
    }

    // ---------- Track status ----------

    @Test fun timelineShowsDecisionButNoBlockchainBeforeFunding() {
        rule.setContent {
            DeedTrackTheme {
                ApplicationDetailScreen(
                    application = application(ApplicationStatus.UNDERWRITER_APPROVED, decision = Decision.APPROVED),
                    onBack = {}, onDocuments = {}, onSimulateNextStatus = {}
                )
            }
        }
        rule.onNodeWithText("AI deliberated").assertIsDisplayed()
        rule.onNodeWithText("Approved").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Pending").assertCountEquals(2)
        rule.onAllNodesWithText("Recorded on blockchain").assertCountEquals(0)
    }

    @Test fun fundedApplicationShowsBlockchainRecord() {
        rule.setContent {
            DeedTrackTheme {
                ApplicationDetailScreen(
                    application = application(ApplicationStatus.FUNDED, decision = Decision.APPROVED, tx = "0xabc123"),
                    onBack = {}, onDocuments = {}, onSimulateNextStatus = {}
                )
            }
        }
        rule.onNodeWithText("Recorded on blockchain").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Transaction ref: 0xabc123").assertIsDisplayed()
    }
}
