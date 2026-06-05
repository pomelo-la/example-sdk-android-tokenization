package com.example.example_google_upp

import android.app.Application
import com.example.example_google_upp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ExampleGoogleUppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ExampleGoogleUppApplication)
            modules(appModule)
        }
    }
}
