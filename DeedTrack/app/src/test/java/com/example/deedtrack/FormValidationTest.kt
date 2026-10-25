package com.example.deedtrack

import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.ui.screens.isStepComplete
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AL3YGP-40 / AL3YGP-56: each step of the application form is validated before continuing. */
class FormValidationTest {

    private val complete = ApplicationDraft(
        employmentStatus = "Employed full-time", employer = "Acme Ltd", yearsEmployed = "6",
        grossIncome = "50000", deposit = "45000",
        propertyAddress = "14 Oakwood Avenue", propertyType = "House", county = "Dublin", askingPrice = "320000",
        loanAmount = "275000", termYears = 30
    )

    @Test fun completeDraftPassesEveryStep() {
        (0..3).forEach { assertTrue("step $it", isStepComplete(it, complete)) }
    }

    @Test fun step1NeedsEmploymentStatusAndYears() {
        assertFalse(isStepComplete(0, complete.copy(employmentStatus = "")))
        assertFalse(isStepComplete(0, complete.copy(yearsEmployed = "")))
    }

    @Test fun step1EmployerOnlyRequiredWhenEmployed() {
        assertFalse(isStepComplete(0, complete.copy(employer = "")))
        assertTrue(isStepComplete(0, complete.copy(employer = "", employmentStatus = "Retired")))
        assertTrue(isStepComplete(0, complete.copy(employer = "", employmentStatus = "Unemployed")))
    }

    @Test fun step2NeedsIncomeAndDeposit() {
        assertFalse(isStepComplete(1, complete.copy(grossIncome = "")))
        assertFalse(isStepComplete(1, complete.copy(deposit = "")))
    }

    @Test fun step3NeedsAddressTypeCountyAndPrice() {
        assertFalse(isStepComplete(2, complete.copy(propertyAddress = "")))
        assertFalse(isStepComplete(2, complete.copy(propertyType = "")))
        assertFalse(isStepComplete(2, complete.copy(county = "")))
        assertFalse(isStepComplete(2, complete.copy(askingPrice = "0")))
    }

    @Test fun step4LoanMustBePositiveAndNotMoreThanPrice() {
        assertFalse(isStepComplete(3, complete.copy(loanAmount = "")))
        assertFalse(isStepComplete(3, complete.copy(loanAmount = "0")))
        assertFalse(isStepComplete(3, complete.copy(loanAmount = "400000")))
        assertTrue(isStepComplete(3, complete.copy(loanAmount = "320000")))
    }
}
