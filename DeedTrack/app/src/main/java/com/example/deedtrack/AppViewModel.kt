package com.example.deedtrack

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.ApplicationStatus
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.PhotoStore
import com.example.deedtrack.data.PropertyListing
import com.example.deedtrack.data.ServiceLocator
import com.example.deedtrack.data.UploadState
import com.example.deedtrack.data.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ServiceLocator.repository
    private val uploader = ServiceLocator.documentUploader
    val valuation = ServiceLocator.valuation

    val user: StateFlow<UserProfile?> = repository.currentUser

    /** Only the logged-in borrower's applications, newest first. */
    val applications: StateFlow<List<MortgageApplication>> =
        combine(repository.applications, repository.currentUser) { apps, user ->
            if (user == null) emptyList()
            else apps.filter { it.ownerEmail.equals(user.email, ignoreCase = true) }
                .sortedByDescending { it.createdAt }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        watchForStatusChanges()
    }

    // ----- Account -----
    suspend fun logIn(email: String, password: String): String? = repository.logIn(email, password)
    suspend fun register(profile: UserProfile, password: String): String? = repository.register(profile, password)
    suspend fun sendPasswordReset(email: String): String? = repository.sendPasswordReset(email)
    fun updateProfile(profile: UserProfile) = repository.updateProfile(profile)

    /** Logs out and removes everything personal that was saved on this phone. */
    fun logOut() {
        repository.logOut()
        repository.clearDeviceData()
        PhotoStore.clear(getApplication())
        _form.value = ApplicationDraft()
        _uploads.value = emptyMap()
    }

    // ----- Property listings (Explore screen) -----
    val listings: List<PropertyListing> = ServiceLocator.listings.listings()

    /** Starts a new application with the chosen property's details already filled in. */
    fun startApplicationFor(listing: PropertyListing) {
        _form.value = listing.toApplicationDraft(repository.draft.value ?: ApplicationDraft())
    }

    // ----- Application form -----
    private val _form = MutableStateFlow(ApplicationDraft())
    val form: StateFlow<ApplicationDraft> = _form.asStateFlow()

    /** Starts a new application, resuming a saved draft if there is one. */
    fun startNewApplication() {
        _form.value = repository.draft.value ?: ApplicationDraft()
    }

    fun updateForm(change: (ApplicationDraft) -> ApplicationDraft) = _form.update(change)

    fun saveDraft() = repository.saveDraft(_form.value)

    fun submitApplication(): MortgageApplication {
        val app = repository.submit(_form.value)
        repository.clearDraft()
        _form.value = ApplicationDraft()
        return app
    }

    // ----- My applications -----
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Pull to refresh on Home. Applications are on the phone for now, so this just re-reads them. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            delay(600)
            _refreshing.value = false
        }
    }

    // ----- Documents -----
    private val _uploads = MutableStateFlow<Map<String, UploadState>>(emptyMap())
    /** Upload progress / errors, keyed by the local file URI. Files not in here are uploaded. */
    val uploads: StateFlow<Map<String, UploadState>> = _uploads.asStateFlow()

    /** Adds the chosen files to the checklist and starts uploading them. */
    fun addDocuments(applicationId: String, type: DocumentType, uris: List<String>) {
        repository.addDocuments(applicationId, type, uris)
        uris.forEach { upload(applicationId, type, it) }
    }

    fun retryUpload(applicationId: String, type: DocumentType, uri: String) = upload(applicationId, type, uri)

    /** Removes a file that failed to upload. */
    fun removeDocument(applicationId: String, type: DocumentType, uri: String) {
        repository.replaceDocument(applicationId, type, uri, null)
        PhotoStore.delete(uri)
        _uploads.update { it - uri }
    }

    private fun upload(applicationId: String, type: DocumentType, uri: String) {
        if (!PhotoStore.isLocal(uri)) return // demo entries / already uploaded
        _uploads.update { it + (uri to UploadState.Uploading(0f)) }
        viewModelScope.launch {
            val result = uploader.upload(applicationId, type, uri) { progress ->
                _uploads.update { it + (uri to UploadState.Uploading(progress)) }
            }
            result.onSuccess { remote ->
                repository.replaceDocument(applicationId, type, uri, remote)
                PhotoStore.delete(uri) // don't keep document images on the phone after upload
                _uploads.update { it - uri }
            }.onFailure { e ->
                _uploads.update { it + (uri to UploadState.Failed(e.message ?: "Upload failed.")) }
            }
        }
    }

    // ----- Status tracking -----
    fun simulateNextStatus(applicationId: String) = repository.advanceStatus(applicationId)

    /** Posts a phone notification whenever one of the borrower's applications changes status. */
    private fun watchForStatusChanges() {
        viewModelScope.launch {
            var known: Map<String, ApplicationStatus>? = null
            applications.collect { apps ->
                val previous = known
                if (previous != null) {
                    apps.filter { previous[it.id] != null && previous[it.id] != it.status }
                        .forEach { StatusNotifications.showStatusChanged(getApplication(), it) }
                }
                known = apps.associate { it.id to it.status }
            }
        }
    }
}
