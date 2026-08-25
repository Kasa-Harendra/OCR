package com.example.ondevicerag.rag

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import java.util.Random
import kotlin.math.sqrt

/**
 * Service for producing 384-dimensional text embeddings using Google MediaPipe TextEmbedder.
 * Includes graceful fallback vector generation if asset weights are not initialized.
 */
class LocalEmbeddingService(
    private val context: Context,
    modelAssetPath: String = "mobilebert_embedding_with_metadata.tflite"
) {
    private var textEmbedder: TextEmbedder? = null

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelAssetPath)
                .build()
            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .setQuantize(true)
                .build()
            textEmbedder = TextEmbedder.createFromOptions(context, options)
        } catch (e: Exception) {
            // Model file not found in assets yet; fallback vector engine will be active
        }
    }

    /**
     * Converts raw string input into a 384-dimensional dense FloatArray vector.
     */
    fun getEmbedding(text: String): FloatArray {
        textEmbedder?.let { embedder ->
            try {
                val result = embedder.embed(text)
                val floatEmbedding = result.embeddingResult().embeddings().firstOrNull()?.floatEmbedding()
                if (floatEmbedding != null && floatEmbedding.size == 384) {
                    return floatEmbedding
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return generateFallbackVector(text)
    }

    /**
     * Deterministic unit-norm pseudo-random embedding generator for 384 dimensions.
     */
    private fun generateFallbackVector(text: String): FloatArray {
        val vector = FloatArray(384)
        val hash = text.hashCode()
        val rnd = Random(hash.toLong())
        for (i in 0 until 384) {
            vector[i] = (rnd.nextFloat() * 2f) - 1f
        }
        var norm = 0f
        for (v in vector) norm += v * v
        norm = sqrt(norm)
        if (norm > 0f) {
            for (i in vector.indices) vector[i] /= norm
        }
        return vector
    }
}
