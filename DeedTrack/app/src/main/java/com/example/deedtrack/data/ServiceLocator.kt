package com.example.deedtrack.data

/** The single place that decides where the app's data comes from. */
object ServiceLocator {
    val repository: AppRepository by lazy { InMemoryAppRepository() }
    val valuation: ValuationService by lazy { PrototypeValuationService() }
}
