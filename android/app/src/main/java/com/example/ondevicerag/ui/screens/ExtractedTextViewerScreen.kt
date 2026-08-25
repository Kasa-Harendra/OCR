package com.example.ondevicerag.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ondevicerag.data.model.DocumentChunk
import com.example.ondevicerag.ui.MainViewModel
import com.example.ondevicerag.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractedTextViewerScreen(
    viewModel: MainViewModel,
    docId: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    
    val selectedDoc = documents.find { it.docId == docId } ?: documents.firstOrNull()
    var selectedPage by remember { mutableStateOf(1) }

    // Fetch chunks for the selected document
    var extractedChunks by remember { mutableStateOf<List<DocumentChunk>>(emptyList()) }

    LaunchedEffect(selectedDoc) {
        selectedDoc?.let { doc ->
            extractedChunks = viewModel.getChunksForDocument(doc.docId)
        }
    }

    val pageNumbers = remember(extractedChunks) {
        extractedChunks.map { it.pageNumber }.distinct().sorted()
    }

    val chunksForPage = remember(extractedChunks, selectedPage) {
        if (pageNumbers.isEmpty()) extractedChunks else extractedChunks.filter { it.pageNumber == selectedPage }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedDoc?.docTitle ?: "Extracted Text Viewer",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${selectedDoc?.totalPages ?: 1} Pages • ${extractedChunks.size} Chunks",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val fullText = chunksForPage.joinToString("\n\n") { it.text }
                            copyToClipboard(context, "Extracted Text", fullText)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", tint = PrimaryCyan)
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
        ) {
            // Page selector tabs
            if (pageNumbers.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageNumbers) { pageNum ->
                        FilterChip(
                            selected = selectedPage == pageNum,
                            onClick = { selectedPage = pageNum },
                            label = { Text("Page $pageNum", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurface,
                                selectedContainerColor = PrimaryCyan.copy(alpha = 0.25f),
                                selectedLabelColor = PrimaryCyan
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Extracted text content list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (chunksForPage.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No extracted text found for this document.", color = TextSecondary)
                        }
                    }
                } else {
                    items(chunksForPage) { chunk ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryCyan.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Page ${chunk.pageNumber} • Chunk ${chunk.id}",
                                            fontSize = 11.sp,
                                            color = PrimaryCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "${chunk.wordCount} words",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = chunk.text,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { copyToClipboard(context, "Chunk Text", chunk.text) }
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = null,
                                            tint = PrimaryCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy Chunk", color = PrimaryCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Extracted text copied to clipboard!", Toast.LENGTH_SHORT).show()
}
