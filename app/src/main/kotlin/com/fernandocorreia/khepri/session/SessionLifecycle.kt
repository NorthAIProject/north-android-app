package com.fernandocorreia.khepri.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.session.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Starts the session at launch, and while it is `Locked` retries each time the app comes forward or the phone is
 * unlocked, the way iOS retries when protected data becomes available.
 */
class SessionLifecycle
@Inject
constructor(private val manager: SessionManager, @ApplicationScope private val scope: CoroutineScope) {
    fun install(context: Context) {
        scope.launch { manager.start() }
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) = retryIfLocked()
                }
            )
        val unlocked =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) = retryIfLocked()
            }
        val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(unlocked, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(unlocked, filter)
        }
    }

    private fun retryIfLocked() {
        if (manager.phase.value == AppPhase.Locked) scope.launch { manager.retry() }
    }
}
