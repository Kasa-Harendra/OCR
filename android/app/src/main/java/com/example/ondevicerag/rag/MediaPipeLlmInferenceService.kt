package com.example.ondevicerag.rag

import android.content.Context
import com.example.ondevicerag.data.model.DocumentChunk
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service managing Google MediaPipe LlmInference for running 4-bit quantized LLM models
 * (e.g., Gemma-2 2B / Llama 3.2 1B) locally on-device.
 */
class MediaPipeLlmInferenceService(
    private val context: Context,
    private val modelPath: String = "gemma-2-2b-it-cpu-int4.task"
) {
    private var llmInference: LlmInference? = null
    var isModelLoaded: Boolean = false
        private set

    init {
        initLlm()
    }

    private fun initLlm() {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.2f)
                .setRandomSeed(42)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
        } catch (e: Exception) {
            // Task bundle file not yet copied to device storage or assets
            isModelLoaded = false
        }
    }

    /**
     * Constructs prompt with source citations and executes local LLM generation.
     */
    suspend fun answerWithCitations(
        userQuery: String,
        matchedChunks: List<DocumentChunk>
    ): String = withContext(Dispatchers.Default) {
        if (matchedChunks.isEmpty()) {
            return@withContext "No relevant document context found in the database. Please ingest documents or refine your question."
        }

        // Format context with source citations [Source: Title, Page: N]
        val contextBlock = matchedChunks.joinToString(separator = "\n\n") { chunk ->
            """
            [Source: ${chunk.docTitle}, Page: ${chunk.pageNumber}]
            ${chunk.text}
            """.trimIndent()
        }

        val prompt = """
            You are an intelligent, helpful on-device assistant. Use ONLY the provided Context to answer the user's question accurately.
            Always cite the document title and page number from the context in your answer.
            
            Context:
            $contextBlock
            
            Question: $userQuery
            
            Answer:
        """.trimIndent()

        llmInference?.let { engine ->
            try {
                val response = engine.generateResponse(prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Synthesize response based on retrieved vector chunks when LLM model weight task file is absent
        return@withContext synthesizeLocalAnswer(userQuery, matchedChunks)
    }

    private fun synthesizeLocalAnswer(query: String, chunks: List<DocumentChunk>): String {
        val sb = StringBuilder()
        sb.append("Based on your indexed documents, here is the answer:\n\n")

        chunks.forEachIndexed { index, chunk ->
            sb.append("${index + 1}. [Source: ${chunk.docTitle}, Page: ${chunk.pageNumber}]\n")
            sb.append("\"${chunk.text}\"\n\n")
        }

        sb.append("Query processed locally using On-Device ObjectBox Vector ANN search.")
        return sb.toString()
    }
}
