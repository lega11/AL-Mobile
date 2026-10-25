package com.example.deedtrack.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.PhotoStore
import com.example.deedtrack.data.UploadState
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.ErrorRed
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SoftFill
import com.example.deedtrack.ui.theme.SuccessBg
import com.example.deedtrack.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Files chosen or photographed, waiting for the borrower to check them before uploading. */
private data class PendingPreview(val type: DocumentType, val uris: List<String>, val fromCamera: Boolean)

/**
 * Document checklist for one application.
 * Shown from "Upload documents" (with a back arrow) and from the Documents tab (with the bottom bar).
 * Files are compressed, previewed, then uploaded with a progress bar and a retry option.
 */
@Composable
fun DocumentChecklistScreen(
    application: MortgageApplication?,
    uploads: Map<String, UploadState>,
    onAddDocuments: (applicationId: String, type: DocumentType, uris: List<String>) -> Unit,
    onRetry: (applicationId: String, type: DocumentType, uri: String) -> Unit,
    onRemove: (applicationId: String, type: DocumentType, uri: String) -> Unit,
    onBack: (() -> Unit)? = null,
    onTab: ((MainTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chooserFor by remember { mutableStateOf<DocumentType?>(null) }
    var preparingFor by remember { mutableStateOf<DocumentType?>(null) }
    var pendingCamera by remember { mutableStateOf<Pair<DocumentType, File>?>(null) }
    var preview by remember { mutableStateOf<PendingPreview?>(null) }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val type = preparingFor
        if (application != null && type != null && uris.isNotEmpty()) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    uris.mapNotNull { PhotoStore.copyIntoAppStorage(context, it)?.toString() }
                }
                preparingFor = null
                if (saved.isNotEmpty()) preview = PendingPreview(type, saved, fromCamera = false)
            }
        } else {
            preparingFor = null
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val pending = pendingCamera
        pendingCamera = null
        if (ok && pending != null && pending.second.length() > 0) {
            preparingFor = pending.first
            scope.launch {
                withContext(Dispatchers.IO) { PhotoStore.compressImage(pending.second) }
                preparingFor = null
                preview = PendingPreview(pending.first, listOf(Uri.fromFile(pending.second).toString()), fromCamera = true)
            }
        } else {
            pending?.second?.delete()
        }
    }

    fun launchCamera(type: DocumentType) {
        val file = PhotoStore.newCameraFile(context)
        pendingCamera = type to file
        takePhoto.launch(PhotoStore.contentUriFor(context, file))
    }

    val done = application?.documentsDone ?: 0
    val total = DocumentType.entries.size

    Scaffold(
        topBar = {
            AppTopBar("Document checklist", onBack = onBack, showBrand = onBack == null, actions = {
                if (application != null) {
                    Text(
                        "$done/$total", color = MutedText,
                        modifier = Modifier.padding(end = 16.dp)
                            .semantics { contentDescription = "$done of $total documents uploaded" }
                    )
                }
            })
        },
        bottomBar = { if (onTab != null) MainBottomBar(MainTab.DOCUMENTS, onTab) }
    ) { padding ->
        if (application == null) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Start a mortgage application to upload your documents.", color = MutedText)
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Outlined.Home)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(application.addressLine, fontWeight = FontWeight.SemiBold)
                            Text(application.reference, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("${done * 100 / total}% complete", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = CardBorder
                )
            }
            items(DocumentType.entries) { type ->
                DocumentRow(
                    type = type,
                    files = application.documents[type].orEmpty(),
                    uploads = uploads,
                    preparing = preparingFor == type,
                    onUpload = { chooserFor = type },
                    onRetry = { uri -> onRetry(application.id, type, uri) },
                    onRemove = { uri -> onRemove(application.id, type, uri) }
                )
            }
            item {
                InfoNote(Icons.Outlined.Info, "PDF, JPG or PNG. Photos are compressed before upload.")
            }
        }
    }

    chooserFor?.let { type ->
        AlertDialog(
            onDismissRequest = { chooserFor = null },
            title = { Text("Upload ${type.label.lowercase()}") },
            text = { Text("Take a clear photo of each page, or choose files from your phone.") },
            confirmButton = {
                TextButton(onClick = {
                    chooserFor = null
                    preparingFor = type
                    pickFiles.launch(arrayOf("image/*", "application/pdf"))
                }) { Text("Choose file / PDF") }
            },
            dismissButton = {
                TextButton(onClick = {
                    chooserFor = null
                    launchCamera(type)
                }) { Text("Take photo") }
            }
        )
    }

    preview?.let { p ->
        PreviewDialog(
            preview = p,
            onUpload = {
                preview = null
                if (application != null) onAddDocuments(application.id, p.type, p.uris)
            },
            onCancel = {
                preview = null
                p.uris.forEach { PhotoStore.delete(it) }
            },
            onRetake = {
                preview = null
                p.uris.forEach { PhotoStore.delete(it) }
                launchCamera(p.type)
            }
        )
    }
}

@Composable
private fun PreviewDialog(preview: PendingPreview, onUpload: () -> Unit, onCancel: () -> Unit, onRetake: () -> Unit) {
    val sizeKb = remember(preview) {
        preview.uris.sumOf { uri -> Uri.parse(uri).path?.let { File(it).length() } ?: 0L } / 1024
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Check your ${preview.type.label.lowercase()}") },
        text = {
            Column {
                Text(
                    "Make sure every page is clear and readable before uploading.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(preview.uris) { uri -> FileThumbnail(uri, Modifier.size(width = 120.dp, height = 160.dp)) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${preview.uris.size} file${if (preview.uris.size == 1) "" else "s"} · $sizeKb KB after compression",
                    style = MaterialTheme.typography.bodySmall, color = MutedText
                )
            }
        },
        confirmButton = { Button(onClick = onUpload) { Text("Upload") } },
        dismissButton = {
            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                if (preview.fromCamera) TextButton(onClick = onRetake) { Text("Retake") }
            }
        }
    )
}

@Composable
private fun FileThumbnail(uri: String, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    if (uri.endsWith(".pdf")) {
        Box(modifier.clip(shape).background(SoftFill), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = "PDF document", tint = ErrorRed, modifier = Modifier.size(40.dp))
        }
    } else {
        AsyncImage(
            model = uri, contentDescription = "Document photo preview",
            contentScale = ContentScale.Crop, modifier = modifier.clip(shape).background(SoftFill)
        )
    }
}

@Composable
private fun DocumentRow(
    type: DocumentType,
    files: List<String>,
    uploads: Map<String, UploadState>,
    preparing: Boolean,
    onUpload: () -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val uploadedCount = files.count { it !in uploads }
    val uploaded = uploadedCount > 0
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uploaded) IconTile(Icons.Filled.CheckCircle, tint = SuccessGreen, background = SuccessBg)
            else IconTile(Icons.Outlined.Description)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    when {
                        uploaded -> "Uploaded · $uploadedCount file${if (uploadedCount == 1) "" else "s"}"
                        files.isNotEmpty() -> "Uploading…"
                        else -> "Missing · Required"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (files.isNotEmpty()) MutedText else ErrorRed
                )
            }
            when {
                preparing -> CircularProgressIndicator(
                    Modifier.size(24.dp).semantics { contentDescription = "Preparing ${type.label}" }, strokeWidth = 2.dp
                )
                files.isNotEmpty() -> TextButton(onClick = onUpload) { Text("Add") }
                else -> OutlinedButton(
                    onClick = onUpload,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) { Text("Upload") }
            }
        }
        // Files still uploading or that failed
        files.filter { it in uploads }.forEachIndexed { index, uri ->
            Spacer(Modifier.height(10.dp))
            when (val state = uploads.getValue(uri)) {
                is UploadState.Uploading -> {
                    Text(
                        "File ${index + 1} · uploading ${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall, color = MutedText
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        trackColor = CardBorder
                    )
                }
                is UploadState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "File ${index + 1} · ${state.message}",
                        style = MaterialTheme.typography.bodySmall, color = ErrorRed, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onRetry(uri) }) { Text("Retry") }
                    TextButton(onClick = { onRemove(uri) }) { Text("Remove") }
                }
            }
        }
    }
}
