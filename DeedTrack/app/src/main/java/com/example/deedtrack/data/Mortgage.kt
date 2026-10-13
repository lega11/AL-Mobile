package com.example.deedtrack.data

import kotlin.math.pow
import kotlin.math.roundToLong

/** Turns typed text like "320,000" into a number. */
fun String.toAmount(): Double = filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0

object MortgageMath {
    /** Illustrative variable rate used for the repayment estimate on the loan step. */
    const val ILLUSTRATIVE_RATE = 3.8

    fun monthlyRepayment(loan: Double, years: Int, annualRatePct: Double = ILLUSTRATIVE_RATE): Double {
        if (loan <= 0 || years <= 0) return 0.0
        val r = annualRatePct / 100 / 12
        val n = years * 12
        return loan * r / (1 - (1 + r).pow(-n.toDouble()))
    }

    fun loanToValue(loan: Double, value: Double): Double = if (value > 0) loan / value * 100 else 0.0
}

/**
 * Property value estimate shown on the property step.
 * Later this will call the AL-AI Engine /predict endpoint (owned by the AI team).
 */
interface ValuationService {
    fun estimate(draft: ApplicationDraft): Double?
}

/** Placeholder until the app is connected to the AL-AI Engine. */
class PrototypeValuationService : ValuationService {
    override fun estimate(draft: ApplicationDraft): Double? {
        val asking = draft.askingPrice.toAmount()
        if (asking <= 0) return null
        return ((asking * 0.953) / 1000).roundToLong() * 1000.0
    }
}
