package com.fernandocorreia.khepri

import android.app.Application
import com.fernandocorreia.khepri.debug.StrictModeInitializer
import com.fernandocorreia.khepri.session.SessionLifecycle
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KhepriApplication : Application() {
    @Inject lateinit var session: SessionLifecycle

    override fun onCreate() {
        if (BuildConfig.DEBUG) StrictModeInitializer.install()
        super.onCreate()
        session.install()
    }
}
