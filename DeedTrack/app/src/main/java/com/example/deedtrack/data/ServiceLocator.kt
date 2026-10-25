package com.example.deedtrack.data

import android.content.Context

/** The single place that decides where the app's data comes from. */
object ServiceLocator {
    /** Applications and drafts kept on the phone (drafts are encrypted, see [SecureDraftStorage]). */
    val localStore = InMemoryAppRepository()

    /** Firebase Auth for accounts; applications are still kept on the phone for now. */
    val repository: AppRepository by lazy { FirebaseAppRepository(localStore) }
    val valuation: ValuationService by lazy { PrototypeValuationService() }
    val listings: PropertyListingsSource = SamplePropertyListings

    /** Sends documents to Firebase Storage until the AL-Core upload endpoint is ready. */
    val documentUploader: DocumentUploader by lazy { FirebaseStorageUploader() }

    /** Called once from MainActivity before any screen is shown. */
    fun init(context: Context) {
        if (localStore.draftStorage == null) {
            localStore.draftStorage = SecureDraftStorage(context.applicationContext)
        }
    }
}
