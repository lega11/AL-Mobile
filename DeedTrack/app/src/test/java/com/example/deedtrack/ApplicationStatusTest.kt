package com.example.deedtrack

import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.data.Decision
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.InMemoryAppRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** AL3YGP-49 / AL3YGP-56: status tracking, decision, on-chain record and documents. */
class ApplicationStatusTest {

    private lateinit var repo: InMemoryAppRepository

    @Before fun setUp() = runBlocking {
        repo = InMemoryAppRepository()
        assertNull(repo.logIn("demo@agenticlender.ie", "password"))
    }

    private fun submitNew() = repo.submit(
        ApplicationDraft(propertyAddress = "14 Oakwood Avenue", county = "Dublin", area = "Dublin 14", loanAmount = "275000")
    )

    @Test fun newApplicationStartsAsApplied() {
        val app = submitNew()
        assertEquals(ApplicationStatus.APPLIED, app.status)
        assertTrue(app.reference.startsWith("AL-IE-"))
        assertEquals("demo@agenticlender.ie", app.ownerEmail)
    }

    @Test fun statusMovesThroughEveryStageInOrder() {
        val id = submitNew().id
        val seen = mutableListOf(ApplicationStatus.APPLIED)
        repeat(4) {
            repo.advanceStatus(id)
            seen += repo.applications.value.first { it.id == id }.status
        }
        assertEquals(ApplicationStatus.entries.toList(), seen)
        repo.advanceStatus(id) // already closed: nothing changes
        assertEquals(ApplicationStatus.CLOSED, repo.applications.value.first { it.id == id }.status)
    }

    @Test fun decisionAndBlockchainRecordAppearAtTheRightStage() {
        val id = submitNew().id
        fun app() = repo.applications.value.first { it.id == id }
        repo.advanceStatus(id) // AI deliberated
        assertNull(app().decision)
        repo.advanceStatus(id) // Underwriter approved
        assertEquals(Decision.APPROVED, app().decision)
        assertNull(app().blockchainTxRef)
        repo.advanceStatus(id) // Funded
        assertNotNull(app().blockchainTxRef)
    }

    @Test fun documentsAreCountedPerType() {
        val id = submitNew().id
        repo.addDocuments(id, DocumentType.PHOTO_ID, listOf("file:///id.jpg"))
        repo.addDocuments(id, DocumentType.PAYSLIPS, listOf("file:///p1.jpg", "file:///p2.jpg"))
        assertEquals(2, repo.applications.value.first { it.id == id }.documentsDone)
    }

    @Test fun uploadedFileReplacesLocalCopy() {
        val id = submitNew().id
        repo.addDocuments(id, DocumentType.PHOTO_ID, listOf("file:///id.jpg"))
        repo.replaceDocument(id, DocumentType.PHOTO_ID, "file:///id.jpg", "gs://bucket/id.jpg")
        assertEquals(listOf("gs://bucket/id.jpg"), repo.applications.value.first { it.id == id }.documents[DocumentType.PHOTO_ID])
        repo.replaceDocument(id, DocumentType.PHOTO_ID, "gs://bucket/id.jpg", null)
        assertEquals(0, repo.applications.value.first { it.id == id }.documentsDone)
    }

    @Test fun areaLineIsBuiltForTheStatusCards() {
        assertEquals("Dublin 14, Co. Dublin", submitNew().areaLine)
    }
}
