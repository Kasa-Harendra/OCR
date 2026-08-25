package com.example.ondevicerag.data

import android.content.Context
import com.example.ondevicerag.data.model.DocumentChunk
import io.objectbox.Box
import io.objectbox.BoxStore

object ObjectBoxManager {
    lateinit var boxStore: BoxStore
        private set

    fun init(context: Context) {
        if (::boxStore.isInitialized && !boxStore.isClosed) {
            return
        }

        try {
            // Reflectively invoke generated MyObjectBox class if available
            val clazz = Class.forName("com.example.ondevicerag.data.model.MyObjectBox")
            val builderMethod = clazz.getMethod("builder")
            val builder = builderMethod.invoke(null)
            
            val androidContextMethod = builder.javaClass.getMethod("androidContext", Any::class.java)
            androidContextMethod.invoke(builder, context.applicationContext)
            
            val buildMethod = builder.javaClass.getMethod("build")
            boxStore = buildMethod.invoke(builder) as BoxStore
        } catch (e: Exception) {
            // Fallback or log if ObjectBox code generation is running during build
            e.printStackTrace()
        }
    }

    val chunkBox: Box<DocumentChunk>
        get() = boxStore.boxFor(DocumentChunk::class.java)
}
