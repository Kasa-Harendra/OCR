package com.example.ondevicerag.data.model

data class TextChunk(
    val index: Int,
    val text: String,
    val wordCount: Int
)

data class ChunkMetadata(
    val docTitle: String,
    val docId: String,
    val pageNumber: Int
)

data class EnrichedChunk(
    val text: String,
    val metadata: ChunkMetadata
)

data class DocumentInfo(
    val docId: String,
    val docTitle: String,
    val totalPages: Int,
    val totalChunks: Int,
    val ingestedTimestamp: Long
)
