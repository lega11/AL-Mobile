package com.example.deedtrack.data

/** The stages a mortgage application moves through (from the project overview). */
enum class ApplicationStatus(val label: String, val description: String) {
    APPLIED("Applied", "We've received your application."),
    AI_DELIBERATED("AI deliberated", "Our AI agents have assessed your application."),
    UNDERWRITER_APPROVED("Underwriter approved", "A bank underwriter has reviewed your application."),
    FUNDED("Funded", "Your loan has been funded and recorded on the blockchain."),
    CLOSED("Closed", "Your application is complete.")
}

enum class Decision(val label: String) {
    APPROVED("Approved"),
    CONDITIONALLY_APPROVED("Conditionally approved"),
    DECLINED("Declined")
}

data class StatusEvent(val status: ApplicationStatus, val timestamp: Long)

enum class DocumentType(val label: String) {
    PHOTO_ID("Photo ID"),
    PAYSLIPS("Last 3 payslips"),
    BANK_STATEMENTS("6 months bank statements"),
    PROPERTY_DOCUMENTS("Property documents")
}

data class UserProfile(
    val fullName: String,
    val email: String,
    val phone: String,
    val dateOfBirth: String = "",
    val address: String = ""
) {
    val initials: String
        get() = fullName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    val firstName: String get() = fullName.split(" ").firstOrNull().orEmpty()
}

/** Everything the borrower enters in the 4-step application form (kept as text while editing). */
data class ApplicationDraft(
    // Step 1 – personal & employment
    val employmentStatus: String = "",
    val employer: String = "",
    val yearsEmployed: String = "",
    val applicants: String = "1",
    val dependants: String = "0",
    // Step 2 – financial
    val grossIncome: String = "",
    val otherIncome: String = "",
    val monthlyRepayments: String = "",
    val creditCardLimits: String = "",
    val deposit: String = "",
    // Step 3 – property
    val propertyAddress: String = "",
    val propertyType: String = "",
    val county: String = "",
    val area: String = "",
    val berRating: String = "",
    val bedrooms: String = "",
    val bathrooms: String = "",
    val floorArea: String = "",
    val askingPrice: String = "",
    // Step 4 – loan
    val loanAmount: String = "",
    val termYears: Int = 30
)

data class MortgageApplication(
    val id: String,
    val reference: String,
    val ownerEmail: String,
    val createdAt: Long,
    val details: ApplicationDraft,
    val history: List<StatusEvent>,
    val decision: Decision? = null,
    val blockchainTxRef: String? = null,
    val documents: Map<DocumentType, List<String>> = emptyMap()
) {
    val status: ApplicationStatus get() = history.last().status
    val addressLine: String get() = details.propertyAddress.ifBlank { "New property" }
    val areaLine: String
        get() = listOf(details.area, details.county.takeIf { it.isNotBlank() }?.let { "Co. $it" })
            .filter { !it.isNullOrBlank() }.joinToString(", ")
    val documentsDone: Int get() = DocumentType.entries.count { documents[it].orEmpty().isNotEmpty() }
}
