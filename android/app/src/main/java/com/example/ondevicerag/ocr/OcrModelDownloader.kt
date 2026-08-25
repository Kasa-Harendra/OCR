package com.example.ondevicerag.ocr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Automatically downloads and extracts specialized Handwritten OCR models (Detection, Recognition & Keys)
 * directly on the Android device during first application launch or OCR engine startup.
 */
object OcrModelDownloader {

    private const val MODELS_ZIP_URL = "https://github.com/hiroi-sora/PaddleOCR-json/releases/download/v1.4.1-dev/models_v1.4.1.zip"
    private const val KEYS_URL = "https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.7/ppocr/utils/ppocr_keys_v1.txt"

    fun getOcrModelDir(context: Context): File {
        val dir = File(context.filesDir, "paddle_ocr")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isModelAvailable(context: Context): Boolean {
        val modelDir = getOcrModelDir(context)
        val files = modelDir.listFiles()
        return !files.isNullOrEmpty() && files.size >= 2
    }

    /**
     * Downloads handwritten OCR model files & dictionary directly over HTTPS on device.
     */
    suspend fun downloadModelsIfNeeded(
        context: Context,
        onProgress: (completed: Int, total: Int, message: String) -> Unit = { _, _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val modelDir = getOcrModelDir(context)

        if (isModelAvailable(context)) {
            onProgress(100, 100, "Handwritten OCR Models ready on device.")
            return@withContext true
        }

        // 1. Download Character Dictionary
        onProgress(10, 100, "Downloading Character Dictionary (ppocr_keys_v1.txt)...")
        val keysFile = File(modelDir, "ppocr_keys_v1.txt")
        downloadFile(KEYS_URL, keysFile)

        // 2. Download Models Zip Archive
        onProgress(30, 100, "Downloading Handwritten OCR models package (58 MB)...")
        val zipFile = File(modelDir, "models_temp.zip")
        val downloaded = downloadFile(MODELS_ZIP_URL, zipFile)

        if (!downloaded) {
            onProgress(0, 100, "Download failed. Will retry on next launch.")
            return@withContext false
        }

        // 3. Extract Zip Archive
        onProgress(85, 100, "Extracting Handwritten OCR models...")
        val extracted = extractZip(zipFile, modelDir)
        if (zipFile.exists()) {
            zipFile.delete()
        }

        onProgress(100, 100, "Handwritten OCR Models successfully installed on device!")
        return@withContext extracted
    }

    private fun downloadFile(urlStr: String, targetFile: File): Boolean {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return false
            }

            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            if (targetFile.exists()) {
                targetFile.delete()
            }
            false
        }
    }

    private fun extractZip(zipFile: File, targetDir: File): Boolean {
        return try {
            ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
