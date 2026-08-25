package com.example.ondevicerag.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ondevicerag.ui.IngestionStatus
import com.example.ondevicerag.ui.MainViewModel
import com.example.ondevicerag.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestionScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val ingestionStatus by viewModel.ingestionStatus.collectAsState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var docTitle by remember { mutableStateOf("") }
    var selectedMimeType by remember { mutableStateOf("application/pdf") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            // Default doc title from uri path
            val path = it.lastPathSegment ?: "document"
            docTitle = path.substringAfterLast("/").substringBeforeLast(".")
            selectedMimeType = context.contentResolver.getType(it) ?: "application/pdf"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Ingestion & OCR", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // File Selection Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = selectedUri?.lastPathSegment ?: "No file selected",
                        color = if (selectedUri != null) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse PDF / Image / Text", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Document Details Section
            OutlinedTextField(
                value = docTitle,
                onValueChange = { docTitle = it },
                label = { Text("Document Title", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Features Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SecondaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Automatic PaddleOCR & Page Splitting",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Scanned pages rendered to images and processed page-by-page or in batches with page number citations.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Status Console & Progress Bar
            when (val status = ingestionStatus) {
                is IngestionStatus.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryCyan, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(status.message, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("${status.progressPercent}%", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { status.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = PrimaryCyan,
                                trackColor = DarkSurfaceVariant
                            )
                        }
                    }
                }
                is IngestionStatus.Success -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusSuccess, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = StatusSuccess.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(status.message, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
                is IngestionStatus.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusError, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = StatusError)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(status.message, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
                IngestionStatus.Idle -> {}
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Ingestion Button
            Button(
                onClick = {
                    selectedUri?.let { uri ->
                        if (docTitle.isNotBlank()) {
                            viewModel.ingestDocument(uri, docTitle, selectedMimeType)
                        }
                    }
                },
                enabled = selectedUri != null && docTitle.isNotBlank() && ingestionStatus !is IngestionStatus.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                Text(
                    text = "Process Document & Build Vector Index",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }
    }
}
