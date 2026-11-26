package com.fernandocorreia.khepri.debug

import android.os.StrictMode

/** Debug builds only: disk or network on the main thread, or a leaked closable, kills the app at once. */
internal object StrictModeInitializer {
    fun install() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().penaltyDeath().build()
        )
    }
}
