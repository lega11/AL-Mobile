package com.example.deedtrack

import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.MortgageMath
import com.example.deedtrack.data.PrototypeValuationService
import com.example.deedtrack.data.toAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** AL3YGP-39 / AL3YGP-42: repayment, loan-to-value and value estimate shown in the form. */
class MortgageMathTest {

    @Test fun parsesTypedAmounts() {
        assertEquals(320000.0, "320,000".toAmount(), 0.0)
        assertEquals(0.0, "".toAmount(), 0.0)
    }

    @Test fun monthlyRepaymentMatchesStandardFormula() {
        assertEquals(1281.38, MortgageMath.monthlyRepayment(275000.0, 30), 0.01)
        assertEquals(0.0, MortgageMath.monthlyRepayment(0.0, 30), 0.0)
    }

    @Test fun loanToValueIsAPercentage() {
        assertEquals(85.9375, MortgageMath.loanToValue(275000.0, 320000.0), 0.0001)
        assertEquals(0.0, MortgageMath.loanToValue(275000.0, 0.0), 0.0)
    }

    @Test fun valueEstimateNeedsAnAskingPrice() {
        val service = PrototypeValuationService()
        assertNull(service.estimate(ApplicationDraft()))
        assertEquals(305000.0, service.estimate(ApplicationDraft(askingPrice = "320000"))!!, 0.0)
    }
}
