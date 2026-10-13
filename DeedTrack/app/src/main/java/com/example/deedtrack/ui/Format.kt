package com.example.deedtrack.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

fun euro(amount: Double): String =
    "€" + NumberFormat.getIntegerInstance(Locale.UK).format(amount.roundToLong())

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.UK).format(Date(millis))
