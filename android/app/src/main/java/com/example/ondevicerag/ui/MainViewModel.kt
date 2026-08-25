package com.example.ondevicerag.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ondevicerag.chunking.DocumentExtractor
import com.example.ondevicerag.data.ObjectBoxManager
import com.example.ondevicerag.data.model.DocumentChunk
import com.example.ondevicerag.data.model.DocumentInfo
import com.example.ondevicerag.ocr.OcrModelDownloader
import com.example.ondevicerag.ocr.PaddleOcrEngine
import com.example.ondevicerag.rag.IngestionManager
import com.example.ondevicerag.rag.LocalEmbeddingService
import com.example.ondevicerag.rag.MediaPipeLlmInferenceService
import com.example.ondevicerag.rag.VectorStoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val matchedChunks: List<DocumentChunk> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

sealed class IngestionStatus {
    object Idle : IngestionStatus()
    data class Loading(val progressPercent: Int, val message: String) : IngestionStatus()
    data class Success(val message: String) : IngestionStatus()
    data class Error(val message: String) : IngestionStatus()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val ocrEngine = PaddleOcrEngine(application)
    private val extractor = DocumentExtractor(application, ocrEngine)
    private val embeddingService = LocalEmbeddingService(application)
    private val vectorStoreService by lazy { VectorStoreService(ObjectBoxManager.boxStore) }
    private val llmService by lazy { MediaPipeLlmInferenceService(application) }

    private val ingestionManager by lazy {
        IngestionManager(
            context = application,
            extractor = extractor,
            embeddingService = embeddingService,
            vectorStore = vectorStoreService
        )
    }

    private val _documents = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val documents: StateFlow<List<DocumentInfo>> = _documents.asStateFlow()

    private val _totalChunks = MutableStateFlow(0L)
    val totalChunks: StateFlow<Long> = _totalChunks.asStateFlow()

    private val _ingestionStatus = MutableStateFlow<IngestionStatus>(IngestionStatus.Idle)
    val ingestionStatus: StateFlow<IngestionStatus> = _ingestionStatus.asStateFlow()

    private val _isModelDownloading = MutableStateFlow(false)
    val isModelDownloading: StateFlow<Boolean> = _isModelDownloading.asStateFlow()

    private val _modelDownloadMessage = MutableStateFlow("")
    val modelDownloadMessage: StateFlow<String> = _modelDownloadMessage.asStateFlow()

    init {
        refreshStats()
        checkAndDownloadOcrModels()
    }

    /**
     * Automatically downloads handwritten OCR models on device if not already cached.
     */
    fun checkAndDownloadOcrModels() {
        if (!OcrModelDownloader.isModelAvailable(getApplication())) {
            viewModelScope.launch {
                _isModelDownloading.value = true
                ocrEngine.prepareHandwrittenModel { completed, total, msg ->
                    _modelDownloadMessage.value = "[$completed/$total] $msg"
                }
                _isModelDownloading.value = false
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            try {
                _documents.value = vectorStoreService.getAllDocuments()
                _totalChunks.value = vectorStoreService.getTotalChunksCount()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getChunksForDocument(docId: String): List<DocumentChunk> {
        return vectorStoreService.getChunksForDocument(docId)
    }

    fun ingestDocument(uri: Uri, docTitle: String, mimeType: String) {
        viewModelScope.launch {
            if (!ocrEngine.isModelReady) {
                _ingestionStatus.value = IngestionStatus.Loading(5, "Downloading Handwritten OCR model assets...")
                ocrEngine.prepareHandwrittenModel()
            }

            _ingestionStatus.value = IngestionStatus.Loading(10, "Starting handwritten text extraction...")
            val success = ingestionManager.ingestDocument(uri, docTitle, mimeType) { percent, msg ->
                _ingestionStatus.value = IngestionStatus.Loading(percent, msg)
            }

            if (success) {
                _ingestionStatus.value = IngestionStatus.Success("Document '$docTitle' handwritten text successfully extracted & saved!")
                refreshStats()
            } else {
                _ingestionStatus.value = IngestionStatus.Error("Failed to extract handwritten text. Please check image clarity.")
            }
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            vectorStoreService.deleteDocument(docId)
            refreshStats()
        }
    }
}
