package com.example.deedtrack.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Accounts use Firebase Authentication, and each borrower's profile is saved
 * in Firestore under users/{uid}.
 *
 * Applications and documents are still kept on the phone by [InMemoryAppRepository]
 * until they are connected to Firestore / Storage.
 */
class FirebaseAppRepository(
    private val local: InMemoryAppRepository = InMemoryAppRepository()
) : AppRepository by local {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Profile entered on the sign-up screen, used while the new account is being created. */
    @Volatile private var pendingProfile: UserProfile? = null

    override val currentUser: StateFlow<UserProfile?> = local.currentUser

    init {
        // Keeps the app in sync with Firebase: also restores the session when the app is reopened.
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                local.setCurrentUser(null)
            } else {
                scope.launch { local.setCurrentUser(loadProfile(user)) }
            }
        }
    }

    override suspend fun logIn(email: String, password: String): String? {
        if (email.isBlank() || password.isBlank()) return "Enter your email and password."
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            null
        } catch (e: Exception) {
            friendlyError(e)
        }
    }

    override suspend fun register(profile: UserProfile, password: String): String? {
        pendingProfile = profile
        return try {
            val result = auth.createUserWithEmailAndPassword(profile.email, password).await()
            val user = result.user ?: return "Could not create your account. Please try again."
            runCatching {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(profile.fullName).build()
                ).await()
            }
            saveProfile(user.uid, profile, isNewAccount = true)
            local.setCurrentUser(profile)
            null
        } catch (e: Exception) {
            pendingProfile = null
            friendlyError(e)
        }
    }

    override suspend fun sendPasswordReset(email: String): String? {
        if (!email.contains("@")) return "Enter a valid email address."
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            null
        } catch (e: Exception) {
            friendlyError(e)
        }
    }

    override fun logOut() {
        pendingProfile = null
        auth.signOut()
        local.setCurrentUser(null)
    }

    override fun updateProfile(profile: UserProfile) {
        local.updateProfile(profile)
        local.setCurrentUser(profile)
        val uid = auth.currentUser?.uid ?: return
        scope.launch { saveProfile(uid, profile, isNewAccount = false) }
    }

    // ---------- Firestore profile (users/{uid}) ----------

    private suspend fun loadProfile(user: FirebaseUser): UserProfile {
        pendingProfile?.let { if (it.email.equals(user.email, ignoreCase = true)) return it }

        val fallback = UserProfile(
            fullName = user.displayName ?: user.email.orEmpty().substringBefore("@"),
            email = user.email.orEmpty(),
            phone = user.phoneNumber.orEmpty()
        )
        val doc = runCatching {
            withTimeoutOrNull(5_000) { db.collection(USERS).document(user.uid).get().await() }
        }.getOrNull() ?: return fallback
        if (!doc.exists()) return fallback

        return UserProfile(
            fullName = doc.getString("fullName") ?: fallback.fullName,
            email = doc.getString("email") ?: fallback.email,
            phone = doc.getString("phone").orEmpty(),
            dateOfBirth = doc.getString("dateOfBirth").orEmpty(),
            address = doc.getString("address").orEmpty()
        )
    }

    private suspend fun saveProfile(uid: String, profile: UserProfile, isNewAccount: Boolean) {
        val data = mutableMapOf<String, Any>(
            "fullName" to profile.fullName,
            "email" to profile.email,
            "phone" to profile.phone,
            "dateOfBirth" to profile.dateOfBirth,
            "address" to profile.address,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isNewAccount) {
            data["consentGiven"] = true
            data["consentAt"] = FieldValue.serverTimestamp()
            data["createdAt"] = FieldValue.serverTimestamp()
        }
        // If Firestore rules or the network block this, the account still works.
        runCatching {
            withTimeoutOrNull(5_000) {
                db.collection(USERS).document(uid).set(data, SetOptions.merge()).await()
            }
        }
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 8 characters."
        is FirebaseAuthInvalidUserException -> "No account found for this email. Create an account first."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account already exists with this email. Try logging in."
        is FirebaseNetworkException -> "No internet connection. Check your connection and try again."
        is FirebaseAuthException ->
            if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED") "Email sign-in isn't turned on in Firebase yet."
            else e.localizedMessage ?: "Something went wrong. Please try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }

    private companion object {
        const val USERS = "users"
    }
}
