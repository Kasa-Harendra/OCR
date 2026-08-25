package com.example.ondevicerag

import android.app.Application
import com.example.ondevicerag.data.ObjectBoxManager

class OnDeviceRagApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize ObjectBox Vector Database
        ObjectBoxManager.init(applicationContext)
    }
}
