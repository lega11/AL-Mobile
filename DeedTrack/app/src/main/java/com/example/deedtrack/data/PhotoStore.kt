package com.example.deedtrack.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Keeps copies of uploaded documents (photos and PDFs) in the app's private storage. */
object PhotoStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "deeds").apply { mkdirs() }

    /** Copies a picked image or PDF into app storage (images are compressed) and returns its file:// URI. */
    fun copyIntoAppStorage(context: Context, source: Uri): Uri? = runCatching {
        val type = context.contentResolver.getType(source).orEmpty()
        val ext = if (type == "application/pdf") "pdf" else "jpg"
        val target = File(dir(context), "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        if (ext == "jpg") compressImage(target)
        Uri.fromFile(target)
    }.getOrNull()

    /**
     * Shrinks a photo so its longest side is at most [maxSide] pixels and saves it as JPEG
     * ([quality] %). Keeps documents readable while making uploads much smaller.
     */
    fun compressImage(file: File, maxSide: Int = 1600, quality: Int = 80) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return
            var sample = 1
            while (longest / (sample * 2) >= maxSide) sample *= 2
            val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return
            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            val output = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            file.outputStream().use { output.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            if (output !== bitmap) output.recycle()
            bitmap.recycle()
        }
    }

    /** True for files kept in the app's private storage (not yet uploaded). */
    fun isLocal(uri: String): Boolean = uri.startsWith("file:")

    /** Deletes a local copy once it has been uploaded. */
    fun delete(uri: String) {
        if (isLocal(uri)) Uri.parse(uri).path?.let { File(it).delete() }
    }

    fun newCameraFile(context: Context): File = File(dir(context), "${UUID.randomUUID()}.jpg")

    fun contentUriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Deletes all stored documents (used on log out). */
    fun clear(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}
