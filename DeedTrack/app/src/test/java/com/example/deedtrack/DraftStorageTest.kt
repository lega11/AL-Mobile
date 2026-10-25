package com.example.deedtrack

import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.DraftCodec
import com.example.deedtrack.data.DraftStorage
import com.example.deedtrack.data.InMemoryAppRepository
import com.example.deedtrack.data.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** AL3YGP-40 / AL3YGP-54 / AL3YGP-56: drafts are saved on the device, resumed, and cleared on log out. */
class DraftStorageTest {

    /** Stands in for the encrypted storage used on the phone. */
    private class FakeDraftStorage : DraftStorage {
        val saved = mutableMapOf<String, String>()
        override fun load(ownerEmail: String) = saved[ownerEmail]?.let { DraftCodec.decode(it) }
        override fun save(ownerEmail: String, draft: ApplicationDraft) { saved[ownerEmail] = DraftCodec.encode(draft) }
        override fun clear(ownerEmail: String) { saved.remove(ownerEmail) }
        override fun clearAll() = saved.clear()
    }

    private val draft = ApplicationDraft(
        employmentStatus = "Self-employed", employer = "Own business", grossIncome = "62000",
        propertyAddress = "2 Main Street, Cork", askingPrice = "295000", loanAmount = "250000", termYears = 25
    )
    private val user = UserProfile("Test Borrower", "test@example.ie", "")

    @Test fun codecRoundTripKeepsEveryField() {
        assertEquals(draft, DraftCodec.decode(DraftCodec.encode(draft)))
    }

    @Test fun codecRejectsDamagedText() {
        assertNull(DraftCodec.decode("not a draft"))
    }

    @Test fun savedDraftIsRestoredAfterAppRestart() = runBlocking {
        val storage = FakeDraftStorage()
        val first = InMemoryAppRepository().apply { draftStorage = storage }
        first.setCurrentUser(user)
        first.saveDraft(draft)

        // New repository = app closed and opened again
        val second = InMemoryAppRepository().apply { draftStorage = storage }
        assertNull(second.draft.value)
        second.setCurrentUser(user)
        assertEquals(draft, second.draft.value)
    }

    @Test fun submittingClearsTheDraft() {
        val storage = FakeDraftStorage()
        val repo = InMemoryAppRepository().apply { draftStorage = storage }
        repo.setCurrentUser(user)
        repo.saveDraft(draft)
        repo.clearDraft()
        assertNull(repo.draft.value)
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun clearDeviceDataRemovesAllDrafts() {
        val storage = FakeDraftStorage()
        val repo = InMemoryAppRepository().apply { draftStorage = storage }
        repo.setCurrentUser(user)
        repo.saveDraft(draft)
        repo.setCurrentUser(null)
        repo.clearDeviceData()
        assertTrue(storage.saved.isEmpty())
        assertNull(repo.draft.value)
    }
}
