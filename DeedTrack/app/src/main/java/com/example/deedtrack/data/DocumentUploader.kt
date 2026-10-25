package com.example.deedtrack.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File

/** Upload state of one document file, shown on the document checklist. */
sealed interface UploadState {
    /** [progress] is 0f..1f */
    data class Uploading(val progress: Float) : UploadState
    data class Failed(val message: String) : UploadState
}

/** Sends a document from the phone to the server. Returns the uploaded file's address. */
interface DocumentUploader {
    suspend fun upload(
        applicationId: String,
        type: DocumentType,
        localUri: String,
        onProgress: (Float) -> Unit
    ): Result<String>
}

/**
 * Uploads to Firebase Storage under users/{uid}/applications/{applicationId}/{type}/.
 * When the AL-Core document endpoint is ready, add another DocumentUploader and
 * switch it in ServiceLocator.
 */
class FirebaseStorageUploader : DocumentUploader {

    override suspend fun upload(
        applicationId: String,
        type: DocumentType,
        localUri: String,
        onProgress: (Float) -> Unit
    ): Result<String> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: error("You need to be logged in to upload documents.")
        val uri = Uri.parse(localUri)
        val name = uri.lastPathSegment ?: File(localUri).name
        val ref = FirebaseStorage.getInstance().reference
            .child("users/$uid/applications/$applicationId/${type.name.lowercase()}/$name")
        val metadata = StorageMetadata.Builder()
            .setContentType(if (name.endsWith(".pdf")) "application/pdf" else "image/jpeg")
            .build()
        ref.putFile(uri, metadata)
            .addOnProgressListener { snap ->
                if (snap.totalByteCount > 0) onProgress(snap.bytesTransferred.toFloat() / snap.totalByteCount)
            }
            .await()
        "gs://${ref.bucket}/${ref.path.trimStart('/')}"
    }.recoverCatching { e ->
        throw IllegalStateException(friendly(e), e)
    }

    private fun friendly(e: Throwable): String {
        val text = e.message.orEmpty()
        return when {
            "logged in" in text -> text
            "network" in text.lowercase() || "timeout" in text.lowercase() -> "No internet connection."
            "permission" in text.lowercase() || "unauthorized" in text.lowercase() -> "Upload not allowed by the server."
            else -> "Upload failed."
        }
    }
}
