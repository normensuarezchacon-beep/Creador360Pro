package com.creador360pro

import android.app.Application
import com.creador360pro.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Creador360App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Creador360App)
            modules(appModule)
        }
    }
}
