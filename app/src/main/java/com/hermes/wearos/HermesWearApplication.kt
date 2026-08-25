package com.hermes.wearos

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HermesWearApplication : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: HermesWearApplication? = null

        fun applicationContext(): Context {
            return instance?.applicationContext ?: throw IllegalStateException("App not initialized")
        }
    }
}
