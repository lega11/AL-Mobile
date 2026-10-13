package com.example.deedtrack.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.deedtrack.data.DocumentType
import com.example.deedtrack.data.MortgageApplication
import com.example.deedtrack.data.PhotoStore
import com.example.deedtrack.ui.components.*
import com.example.deedtrack.ui.theme.CardBorder
import com.example.deedtrack.ui.theme.ErrorRed
import com.example.deedtrack.ui.theme.MutedText
import com.example.deedtrack.ui.theme.SuccessBg
import com.example.deedtrack.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Document checklist for one application.
 * Shown from "Upload documents" (with a back arrow) and from the Documents tab (with the bottom bar).
 */
@Composable
fun DocumentChecklistScreen(
    application: MortgageApplication?,
    onAddDocuments: (applicationId: String, type: DocumentType, uris: List<String>) -> Unit,
    onBack: (() -> Unit)? = null,
    onTab: ((MainTab) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chooserFor by remember { mutableStateOf<DocumentType?>(null) }
    var uploadingFor by remember { mutableStateOf<DocumentType?>(null) }
    var pendingCamera by remember { mutableStateOf<Pair<DocumentType, File>?>(null) }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        val type = uploadingFor
        if (application != null && type != null && uris.isNotEmpty()) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    uris.mapNotNull { PhotoStore.copyIntoAppStorage(context, it)?.toString() }
                }
                onAddDocuments(application.id, type, saved)
                uploadingFor = null
            }
        } else {
            uploadingFor = null
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val pending = pendingCamera
        if (ok && pending != null && application != null && pending.second.length() > 0) {
            onAddDocuments(application.id, pending.first, listOf(Uri.fromFile(pending.second).toString()))
        } else {
            pending?.second?.delete()
        }
        pendingCamera = null
    }

    val done = application?.documentsDone ?: 0
    val total = DocumentType.entries.size

    Scaffold(
        topBar = {
            AppTopBar("Document checklist", onBack = onBack, showBrand = onBack == null, actions = {
                if (application != null) {
                    Text("$done/$total", color = MutedText, modifier = Modifier.padding(end = 16.dp))
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
                val files = application.documents[type].orEmpty()
                DocumentRow(
                    type = type,
                    fileCount = files.size,
                    uploading = uploadingFor == type,
                    onUpload = { chooserFor = type }
                )
            }
            item {
                InfoNote(Icons.Outlined.Info, "PDF, JPG or PNG. Maximum file size 10 MB.")
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
                    uploadingFor = type
                    pickFiles.launch(arrayOf("image/*", "application/pdf"))
                }) { Text("Choose file / PDF") }
            },
            dismissButton = {
                TextButton(onClick = {
                    chooserFor = null
                    val file = PhotoStore.newCameraFile(context)
                    pendingCamera = type to file
                    takePhoto.launch(PhotoStore.contentUriFor(context, file))
                }) { Text("Take photo") }
            }
        )
    }
}

@Composable
private fun DocumentRow(type: DocumentType, fileCount: Int, uploading: Boolean, onUpload: () -> Unit) {
    val uploaded = fileCount > 0
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uploaded) IconTile(Icons.Filled.CheckCircle, tint = SuccessGreen, background = SuccessBg)
            else IconTile(Icons.Outlined.Description)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(type.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (uploaded) "Uploaded · $fileCount file${if (fileCount == 1) "" else "s"}" else "Missing · Required",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uploaded) MutedText else ErrorRed
                )
            }
            when {
                uploading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                uploaded -> TextButton(onClick = onUpload) { Text("Add") }
                else -> OutlinedButton(
                    onClick = onUpload,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) { Text("Upload") }
            }
        }
    }
}
