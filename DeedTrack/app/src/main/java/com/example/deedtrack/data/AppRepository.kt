package com.example.deedtrack.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

/**
 * Everything the screens need from outside the app.
 * Right now it's all kept in memory on the phone (prototype).
 * When AL-Core is ready, write a class that implements this interface and
 * change the one line in ServiceLocator – no screens need to change.
 */
interface AppRepository {
    val currentUser: StateFlow<UserProfile?>
    val applications: StateFlow<List<MortgageApplication>>
    val draft: StateFlow<ApplicationDraft?>

    /** Returns an error message, or null if logged in. */
    suspend fun logIn(email: String, password: String): String?
    /** Returns an error message, or null if the account was created. */
    suspend fun register(profile: UserProfile, password: String): String?
    /** Returns an error message, or null if the reset email was sent. */
    suspend fun sendPasswordReset(email: String): String?
    fun logOut()
    fun updateProfile(profile: UserProfile)

    fun saveDraft(draft: ApplicationDraft)
    fun clearDraft()
    fun submit(draft: ApplicationDraft): MortgageApplication
    fun addDocuments(applicationId: String, type: DocumentType, uris: List<String>)
    /** Swaps a local file for its uploaded copy (or removes it when [newUri] is null). */
    fun replaceDocument(applicationId: String, type: DocumentType, oldUri: String, newUri: String?)

    /** Removes drafts and other personal data saved on this phone (used on log out). */
    fun clearDeviceData()

    /** Demo only: moves an application to the next status (normally done by AL-Web / AL-Core). */
    fun advanceStatus(applicationId: String)
}

class InMemoryAppRepository : AppRepository {

    private data class Account(val profile: UserProfile, val password: String)

    private val accounts = mutableMapOf<String, Account>()

    /** Encrypted on-device storage for drafts (set by MainActivity; null in unit tests). */
    var draftStorage: DraftStorage? = null
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    private val _applications = MutableStateFlow<List<MortgageApplication>>(emptyList())
    private val _draft = MutableStateFlow<ApplicationDraft?>(null)

    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()
    override val applications: StateFlow<List<MortgageApplication>> = _applications.asStateFlow()
    override val draft: StateFlow<ApplicationDraft?> = _draft.asStateFlow()

    init {
        seedDemoAccount()
    }

    /** Used by FirebaseAppRepository to tell this class who is logged in. */
    fun setCurrentUser(profile: UserProfile?) {
        _currentUser.value = profile
        _draft.value = profile?.let { draftStorage?.load(it.email) }
    }

    override suspend fun logIn(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) return "Enter your email and password."
        val account = accounts[email.lowercase()] ?: return "No account found for this email. Create an account first."
        if (account.password != password) return "Incorrect email or password."
        setCurrentUser(account.profile)
        return null
    }

    override suspend fun register(profile: UserProfile, password: String): String? {
        accounts[profile.email.lowercase()] = Account(profile, password)
        _currentUser.value = profile
        return null
    }

    override suspend fun sendPasswordReset(email: String): String? = null

    override fun logOut() {
        _currentUser.value = null
        _draft.value = null
    }

    override fun updateProfile(profile: UserProfile) {
        val key = profile.email.lowercase()
        accounts[key]?.let { accounts[key] = it.copy(profile = profile) }
        _currentUser.value = profile
    }

    override fun saveDraft(draft: ApplicationDraft) {
        _draft.value = draft
        _currentUser.value?.let { draftStorage?.save(it.email, draft) }
    }

    override fun clearDraft() {
        _draft.value = null
        _currentUser.value?.let { draftStorage?.clear(it.email) }
    }

    override fun clearDeviceData() {
        _draft.value = null
        draftStorage?.clearAll()
    }

    override fun submit(draft: ApplicationDraft): MortgageApplication {
        val now = System.currentTimeMillis()
        val app = MortgageApplication(
            id = UUID.randomUUID().toString(),
            reference = newReference(),
            ownerEmail = _currentUser.value?.email.orEmpty(),
            createdAt = now,
            details = draft,
            history = listOf(StatusEvent(ApplicationStatus.APPLIED, now))
        )
        _applications.update { listOf(app) + it }
        return app
    }

    override fun addDocuments(applicationId: String, type: DocumentType, uris: List<String>) {
        _applications.update { list ->
            list.map { app ->
                if (app.id != applicationId) app
                else app.copy(documents = app.documents + (type to (app.documents[type].orEmpty() + uris)))
            }
        }
    }

    override fun replaceDocument(applicationId: String, type: DocumentType, oldUri: String, newUri: String?) {
        _applications.update { list ->
            list.map { app ->
                if (app.id != applicationId) app
                else {
                    val files = app.documents[type].orEmpty()
                        .mapNotNull { if (it == oldUri) newUri else it }
                    app.copy(documents = if (files.isEmpty()) app.documents - type else app.documents + (type to files))
                }
            }
        }
    }

    override fun advanceStatus(applicationId: String) {
        _applications.update { list ->
            list.map { app ->
                if (app.id != applicationId) return@map app
                val next = ApplicationStatus.entries.getOrNull(app.status.ordinal + 1) ?: return@map app
                app.copy(
                    history = app.history + StatusEvent(next, System.currentTimeMillis()),
                    decision = if (next == ApplicationStatus.UNDERWRITER_APPROVED) Decision.APPROVED else app.decision,
                    blockchainTxRef = if (next == ApplicationStatus.FUNDED) newTxRef() else app.blockchainTxRef
                )
            }
        }
    }

    private fun newReference(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "AL-IE-$year-${Random.nextInt(10000, 99999)}"
    }

    private fun newTxRef(): String =
        "0x" + (1..10).joinToString("") { "0123456789abcdef"[Random.nextInt(16)].toString() } + "…"

    /** Demo login: demo@agenticlender.ie / password */
    private fun seedDemoAccount() {
        val demo = UserProfile(
            fullName = "Eimear Murphy",
            email = "demo@agenticlender.ie",
            phone = "+353 87 123 4567",
            dateOfBirth = "14 September 1990",
            address = "8 River Walk, Dublin 6"
        )
        accounts[demo.email] = Account(demo, "password")
        val created = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
        _applications.value = listOf(
            MortgageApplication(
                id = "demo-1",
                reference = "AL-IE-2025-10482",
                ownerEmail = demo.email,
                createdAt = created,
                details = ApplicationDraft(
                    employmentStatus = "Employed full-time", employer = "Acme Ltd", yearsEmployed = "6",
                    grossIncome = "50000", otherIncome = "0", monthlyRepayments = "450",
                    creditCardLimits = "5000", deposit = "45000",
                    propertyAddress = "14 Oakwood Avenue", propertyType = "House", county = "Dublin",
                    area = "Dublin 14", berRating = "B2", bedrooms = "3", bathrooms = "2",
                    floorArea = "104", askingPrice = "320000", loanAmount = "275000", termYears = 30
                ),
                history = listOf(
                    StatusEvent(ApplicationStatus.APPLIED, created),
                    StatusEvent(ApplicationStatus.AI_DELIBERATED, created + 6 * 60 * 60 * 1000L)
                ),
                documents = mapOf(
                    DocumentType.PHOTO_ID to listOf("demo"),
                    DocumentType.PAYSLIPS to listOf("demo", "demo", "demo")
                )
            )
        )
    }
}
