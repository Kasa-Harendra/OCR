package com.example.ondevicerag.rag

import android.content.Context
import android.net.Uri
import com.example.ondevicerag.chunking.DocumentExtractor
import com.example.ondevicerag.data.model.EnrichedChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Orchestrates complete ingestion workflow:
 * Document Selection / Camera Capture -> Page Separation -> PaddleOCR Processing ->
 * Recursive Chunking -> Local Vector Embedding -> ObjectBox Persistent Storage.
 */
class IngestionManager(
    private val context: Context,
    private val extractor: DocumentExtractor,
    private val embeddingService: LocalEmbeddingService,
    private val vectorStore: VectorStoreService
) {

    suspend fun ingestDocument(
        uri: Uri,
        docTitle: String,
        mimeType: String,
        onProgress: (completedPercent: Int, statusMessage: String) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val docId = UUID.randomUUID().toString()
        onProgress(10, "Extracting pages & reading content...")

        val enrichedChunks: List<EnrichedChunk> = when {
            mimeType.contains("pdf", ignoreCase = true) -> {
                extractor.extractPdfWithMetadata(
                    uri = uri,
                    docTitle = docTitle,
                    docId = docId
                ) { completed, total ->
                    val percent = 10 + (completed * 40 / maxOf(total, 1))
                    onProgress(percent, "Processing PDF page $completed of $total")
                }
            }
            mimeType.startsWith("image/", ignoreCase = true) -> {
                onProgress(20, "Running PaddleOCR on image document...")
                extractor.extractImageWithMetadata(uri, docTitle, docId)
            }
            else -> {
                onProgress(20, "Parsing plain text file...")
                extractor.extractPlainTextWithMetadata(uri, docTitle, docId)
            }
        }

        if (enrichedChunks.isEmpty()) {
            onProgress(0, "Extraction failed: No text or readable pages found.")
            return@withContext false
        }

        onProgress(55, "Generating 384-dimensional vector embeddings...")
        val chunksWithVectors = enrichedChunks.mapIndexed { index, chunk ->
            val vector = embeddingService.getEmbedding(chunk.text)
            val currentPercent = 55 + ((index + 1) * 35 / enrichedChunks.size)
            onProgress(currentPercent, "Embedding chunk ${index + 1}/${enrichedChunks.size}")
            Pair(chunk, vector)
        }

        onProgress(92, "Persisting chunks & page metadata into ObjectBox DB...")
        vectorStore.indexEnrichedChunks(chunksWithVectors)

        onProgress(100, "Successfully ingested ${enrichedChunks.size} chunks from '$docTitle'!")
        return@withContext true
    }
}
