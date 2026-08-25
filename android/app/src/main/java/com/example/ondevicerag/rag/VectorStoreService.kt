package com.example.ondevicerag.rag

import com.example.ondevicerag.data.model.DocumentChunk
import com.example.ondevicerag.data.model.DocumentInfo
import com.example.ondevicerag.data.model.EnrichedChunk
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder

/**
 * Service managing ObjectBox database operations including document storage,
 * text chunk queries, metadata filtering, and retrieval.
 */
class VectorStoreService(private val boxStore: BoxStore) {
    private val chunkBox: Box<DocumentChunk> = boxStore.boxFor(DocumentChunk::class.java)

    /**
     * Batch inserts enriched chunks with metadata inside a transaction.
     */
    fun indexEnrichedChunks(chunksWithVectors: List<Pair<EnrichedChunk, FloatArray>>) {
        boxStore.runInTx {
            chunksWithVectors.forEach { (chunk, vector) ->
                val wordCount = chunk.text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                chunkBox.put(
                    DocumentChunk(
                        text = chunk.text,
                        docTitle = chunk.metadata.docTitle,
                        docId = chunk.metadata.docId,
                        pageNumber = chunk.metadata.pageNumber,
                        wordCount = wordCount,
                        embedding = vector
                    )
                )
            }
        }
    }

    /**
     * Fetches all extracted text chunks for a specific document ID ordered by page number.
     */
    fun getChunksForDocument(docId: String): List<DocumentChunk> {
        return chunkBox.all
            .filter { it.docId.equals(docId, ignoreCase = true) }
            .sortedWith(compareBy({ it.pageNumber }, { it.id }))
    }

    /**
     * Performs vector search combined with optional metadata filters (docId, page range).
     */
    fun retrieveTopKFiltered(
        queryVector: FloatArray,
        topK: Long = 3,
        targetDocId: String? = null,
        minPage: Int? = null,
        maxPage: Int? = null
    ): List<DocumentChunk> {
        val allChunks = chunkBox.all
        if (allChunks.isEmpty()) return emptyList()

        val filtered = allChunks.filter { chunk ->
            val matchDoc = targetDocId.isNullOrBlank() || chunk.docId.equals(targetDocId, ignoreCase = true)
            val matchMinPage = minPage == null || chunk.pageNumber >= minPage
            val matchMaxPage = maxPage == null || chunk.pageNumber <= maxPage
            matchDoc && matchMinPage && matchMaxPage
        }

        val ranked = filtered.map { chunk ->
            val dist = computeCosineDistance(queryVector, chunk.embedding)
            Pair(chunk, dist)
        }.sortedBy { it.second }

        return ranked.take(topK.toInt()).map { it.first }
    }

    /**
     * Retrieves high-level metadata for all ingested documents in the persistent store.
     */
    fun getAllDocuments(): List<DocumentInfo> {
        val allChunks = chunkBox.all
        if (allChunks.isEmpty()) return emptyList()

        val docGroups = allChunks.groupBy { it.docId }
        return docGroups.map { (docId, chunks) ->
            val first = chunks.first()
            val totalPages = chunks.maxOfOrNull { it.pageNumber } ?: 1
            DocumentInfo(
                docId = docId,
                docTitle = first.docTitle,
                totalPages = totalPages,
                totalChunks = chunks.size,
                ingestedTimestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Deletes all chunks associated with a document ID.
     */
    fun deleteDocument(docId: String) {
        val chunksToDelete = chunkBox.all.filter { it.docId.equals(docId, ignoreCase = true) }
        chunkBox.remove(chunksToDelete)
    }

    /**
     * Returns total stored chunks in the database.
     */
    fun getTotalChunksCount(): Long {
        return chunkBox.count()
    }

    private fun computeCosineDistance(v1: FloatArray, v2: FloatArray?): Float {
        if (v2 == null || v1.size != v2.size) return 1.0f

        var dot = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }

        val denominator = (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        if (denominator == 0.0f) return 1.0f

        val similarity = dot / denominator
        return 1.0f - similarity
    }
}
