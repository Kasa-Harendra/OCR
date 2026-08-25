package com.example.ondevicerag.data.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.HnswIndex
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class DocumentChunk(
    @Id var id: Long = 0,
    var text: String = "",
    
    // Metadata properties for filtering & citation
    @Index var docTitle: String = "",
    @Index var docId: String = "",
    @Index var pageNumber: Int = 1,
    var wordCount: Int = 0,
    
    // Vector embedding property for HNSW Approximate Nearest Neighbor search
    @HnswIndex(dimensions = 384)
    var embedding: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocumentChunk

        if (id != other.id) return false
        if (text != other.text) return false
        if (docTitle != other.docTitle) return false
        if (docId != other.docId) return false
        if (pageNumber != other.pageNumber) return false
        if (wordCount != other.wordCount) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + docTitle.hashCode()
        result = 31 * result + docId.hashCode()
        result = 31 * result + pageNumber.hashCode()
        result = 31 * result + wordCount
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
