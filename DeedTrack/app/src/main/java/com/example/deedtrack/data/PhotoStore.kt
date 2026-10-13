package com.example.deedtrack.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Keeps copies of uploaded documents (photos and PDFs) in the app's private storage. */
object PhotoStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "deeds").apply { mkdirs() }

    /** Copies a picked image or PDF into app storage and returns its file:// URI. */
    fun copyIntoAppStorage(context: Context, source: Uri): Uri? = runCatching {
        val type = context.contentResolver.getType(source).orEmpty()
        val ext = if (type == "application/pdf") "pdf" else "jpg"
        val target = File(dir(context), "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(target)
    }.getOrNull()

    fun newCameraFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.jpg")

    fun contentUriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Deletes all stored documents (used on log out). */
    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}
