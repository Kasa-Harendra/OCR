package com.example.ondevicerag.chunking

import com.example.ondevicerag.data.model.TextChunk

/**
 * Recursive Semantic Chunker that respects natural linguistic boundaries:
 * Paragraphs (\n\n) -> Sentences (. , ! , ?) -> Words (space)
 * with customizable maximum chunk character size and overlap.
 */
class RecursiveSemanticChunker(
    private val maxChunkSize: Int = 600,
    private val chunkOverlap: Int = 100
) {
    private val separators = listOf("\n\n", "\n", ". ", "! ", "? ", " ")

    fun split(text: String): List<TextChunk> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val rawSplits = splitRecursively(trimmed, separators)
        return mergeAndOverlapChunks(rawSplits)
    }

    private fun splitRecursively(text: String, currentSeparators: List<String>): List<String> {
        if (text.length <= maxChunkSize || currentSeparators.isEmpty()) {
            return listOf(text)
        }

        val sep = currentSeparators.first()
        val nextSeps = currentSeparators.drop(1)
        val splits = text.split(sep)

        val result = mutableListOf<String>()
        for (split in splits) {
            if (split.length <= maxChunkSize) {
                if (split.isNotBlank()) result.add(split)
            } else {
                result.addAll(splitRecursively(split, nextSeps))
            }
        }
        return result
    }

    private fun mergeAndOverlapChunks(splits: List<String>): List<TextChunk> {
        val merged = mutableListOf<TextChunk>()
        var current = StringBuilder()
        var chunkIndex = 0

        for (split in splits) {
            if (current.length + split.length + 1 > maxChunkSize) {
                if (current.isNotEmpty()) {
                    val chunkText = current.toString().trim()
                    if (chunkText.isNotEmpty()) {
                        merged.add(
                            TextChunk(
                                index = chunkIndex++,
                                text = chunkText,
                                wordCount = chunkText.split(Regex("\\s+")).size
                            )
                        )
                    }
                }
                
                // Extract overlapping tail from previous chunk
                val overlapStart = maxOf(0, current.length - chunkOverlap)
                val overlapText = current.substring(overlapStart)
                current = StringBuilder(overlapText)
                if (current.isNotEmpty() && !current.endsWith(" ")) {
                    current.append(" ")
                }
            }
            current.append(split).append(" ")
        }

        if (current.isNotEmpty()) {
            val chunkText = current.toString().trim()
            if (chunkText.isNotEmpty()) {
                merged.add(
                    TextChunk(
                        index = chunkIndex,
                        text = chunkText,
                        wordCount = chunkText.split(Regex("\\s+")).size
                    )
                )
            }
        }

        return merged
    }
}
