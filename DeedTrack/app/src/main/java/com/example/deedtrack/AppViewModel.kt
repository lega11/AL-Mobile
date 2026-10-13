package com.example.deedtrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.deedtrack.data.ApplicationDraft
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.ServiceLocator
import com.example.deedtrack.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class AppViewModel : ViewModel() {

    private val repository = ServiceLocator.repository
    val valuation = ServiceLocator.valuation

    val user: StateFlow<UserProfile?> = repository.currentUser

    /** Only the logged-in borrower's applications, newest first. */
    val applications: StateFlow<List<MortgageApplication>> =
        combine(repository.applications, repository.currentUser) { apps, user ->
            if (user == null) emptyList()
            else apps.filter { it.ownerEmail.equals(user.email, ignoreCase = true) }
                .sortedByDescending { it.createdAt }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ----- Account -----
    fun logIn(email: String, password: String): String? = repository.logIn(email, password)
    fun register(profile: UserProfile, password: String) = repository.register(profile, password)
    fun logOut() = repository.logOut()
    fun updateProfile(profile: UserProfile) = repository.updateProfile(profile)

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

    // ----- Documents & status -----
    fun addDocuments(applicationId: String, type: DocumentType, uris: List<String>) =
        repository.addDocuments(applicationId, type, uris)

    fun simulateNextStatus(applicationId: String) = repository.advanceStatus(applicationId)
}
