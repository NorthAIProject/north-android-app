package com.fernandocorreia.khepri

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The merged manifest. Without INTERNET every call fails as "check your connection" and nothing reaches the server. */
@RunWith(AndroidJUnit4::class)
class ManifestTest {
    @Test
    fun `the app may open sockets`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(Manifest.permission.INTERNET in info.requestedPermissions.orEmpty())
    }
}
