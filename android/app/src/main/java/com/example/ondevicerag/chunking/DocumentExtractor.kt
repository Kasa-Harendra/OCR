package com.example.ondevicerag.chunking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.ondevicerag.data.model.ChunkMetadata
import com.example.ondevicerag.data.model.EnrichedChunk
import com.example.ondevicerag.ocr.PaddleOcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Extracts and parses documents (PDFs, Plain Text, Images, Camera Captures) page-by-page.
 * Uses Android native [PdfRenderer] with 3.0x high-DPI resolution rendering for maximum OCR accuracy.
 */
class DocumentExtractor(
    private val context: Context,
    private val ocrEngine: PaddleOcrEngine
) {
    private val chunker = RecursiveSemanticChunker()

    /**
     * Renders multi-page PDF documents page-by-page with 3.0x high DPI resolution rendering,
     * passes rendered page Bitmaps to [PaddleOcrEngine], and chunks extracted handwritten text.
     */
    suspend fun extractPdfWithMetadata(
        uri: Uri,
        docTitle: String,
        docId: String,
        batchSize: Int = 2,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): List<EnrichedChunk> = withContext(Dispatchers.IO) {
        val results = mutableListOf<EnrichedChunk>()
        val fileDescriptor = getFileDescriptorFromUri(uri) ?: return@withContext emptyList()

        try {
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val totalPages = pdfRenderer.pageCount

            for (pageIndex in 0 until totalPages) {
                val page = pdfRenderer.openPage(pageIndex)
                
                // Render page to ultra high-resolution bitmap (3.0x DPI) for accurate handwritten OCR
                val width = (page.width * 3.0).toInt()
                val height = (page.height * 3.0).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Process rendered handwritten page frame using OCR engine
                val pageText = ocrEngine.processPage(bitmap)
                bitmap.recycle()

                if (pageText.isNotBlank()) {
                    val chunks = chunker.split(pageText)
                    chunks.forEach { chunk ->
                        results.add(
                            EnrichedChunk(
                                text = chunk.text,
                                metadata = ChunkMetadata(
                                    docTitle = docTitle,
                                    docId = docId,
                                    pageNumber = pageIndex + 1
                                )
                            )
                        )
                    }
                }
                onProgress(pageIndex + 1, totalPages)
            }
            pdfRenderer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext results
    }

    /**
     * Extracts text from an image (Gallery selection or Camera capture) using PaddleOCR.
     */
    suspend fun extractImageWithMetadata(
        uri: Uri,
        docTitle: String,
        docId: String,
        pageNumber: Int = 1
    ): List<EnrichedChunk> = withContext(Dispatchers.IO) {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        val bitmap = BitmapFactory.decodeStream(stream) ?: return@withContext emptyList()
        
        val ocrText = ocrEngine.processPage(bitmap)
        bitmap.recycle()

        if (ocrText.isBlank()) return@withContext emptyList()

        val chunks = chunker.split(ocrText)
        return@withContext chunks.map { chunk ->
            EnrichedChunk(
                text = chunk.text,
                metadata = ChunkMetadata(
                    docTitle = docTitle,
                    docId = docId,
                    pageNumber = pageNumber
                )
            )
        }
    }

    /**
     * Extracts text from a raw text document Uri.
     */
    suspend fun extractPlainTextWithMetadata(
        uri: Uri,
        docTitle: String,
        docId: String
    ): List<EnrichedChunk> = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        val text = stream.bufferedReader().use { it.readText() }
        
        if (text.isBlank()) return@withContext emptyList()

        val chunks = chunker.split(text)
        return@withContext chunks.map { chunk ->
            EnrichedChunk(
                text = chunk.text,
                metadata = ChunkMetadata(
                    docTitle = docTitle,
                    docId = docId,
                    pageNumber = 1
                )
            )
        }
    }

    private fun getFileDescriptorFromUri(uri: Uri): ParcelFileDescriptor? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            try {
                val tempFile = File.createTempFile("temp_doc_", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }
}
