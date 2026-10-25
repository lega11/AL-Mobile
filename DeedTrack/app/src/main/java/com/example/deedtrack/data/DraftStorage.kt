package com.example.deedtrack.data

/** Where in-progress applications are kept between app launches. */
interface DraftStorage {
    fun load(ownerEmail: String): ApplicationDraft?
    fun save(ownerEmail: String, draft: ApplicationDraft)
    fun clear(ownerEmail: String)
    fun clearAll()
}

/**
 * Turns a draft into a single line of text and back, so it can be encrypted and saved.
 * Plain Kotlin (no Android classes) so it can be unit tested.
 */
object DraftCodec {
    private const val SEP = '\u001F'

    fun encode(d: ApplicationDraft): String = listOf(
        d.employmentStatus, d.employer, d.yearsEmployed, d.applicants, d.dependants,
        d.grossIncome, d.otherIncome, d.monthlyRepayments, d.creditCardLimits, d.deposit,
        d.propertyAddress, d.propertyType, d.county, d.area, d.berRating, d.bedrooms, d.bathrooms,
        d.floorArea, d.askingPrice, d.loanAmount, d.termYears.toString()
    ).joinToString(SEP.toString()) { it.replace(SEP, ' ') }

    fun decode(text: String): ApplicationDraft? {
        val f = text.split(SEP)
        if (f.size != 21) return null
        return ApplicationDraft(
            employmentStatus = f[0], employer = f[1], yearsEmployed = f[2], applicants = f[3], dependants = f[4],
            grossIncome = f[5], otherIncome = f[6], monthlyRepayments = f[7], creditCardLimits = f[8], deposit = f[9],
            propertyAddress = f[10], propertyType = f[11], county = f[12], area = f[13], berRating = f[14],
            bedrooms = f[15], bathrooms = f[16], floorArea = f[17], askingPrice = f[18], loanAmount = f[19],
            termYears = f[20].toIntOrNull() ?: 30
        )
    }
}
