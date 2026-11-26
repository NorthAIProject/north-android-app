package com.fernandocorreia.khepri

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.ui.LocalReduceMotion
import com.fernandocorreia.khepri.shell.FailedScreen
import com.fernandocorreia.khepri.shell.LockedScreen
import com.fernandocorreia.khepri.shell.OnboardingPlaceholder
import com.fernandocorreia.khepri.shell.coveredByConsent

private const val PHASE_FADE_MS = 250

/**
 * iOS `RootView`: one screen per phase, cross-faded as iOS animates the phase change, under the AI consent question.
 */
@Composable
internal fun RootContent(
    phase: AppPhase,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    signedOut: @Composable () -> Unit,
    signedIn: @Composable () -> Unit,
    asksConsent: Boolean,
    consent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Phases(phase, onRetry, onSignOut, signedOut, signedIn)
        // Over the signed-in app and onboarding only: a sign-out underneath takes the question away with it.
        if (asksConsent && phase.coveredByConsent()) consent()
    }
}

@Composable
private fun Phases(
    phase: AppPhase,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    signedOut: @Composable () -> Unit,
    signedIn: @Composable () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            val spec = if (reduceMotion) snap<Float>() else tween(PHASE_FADE_MS)
            fadeIn(spec) togetherWith fadeOut(spec)
        },
        contentKey = { it.stateKey() },
    ) { shown ->
        when (shown) {
            AppPhase.Launching ->
                Box(Modifier.fillMaxSize().testTag("phase-launching"), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            AppPhase.Locked -> LockedScreen()
            AppPhase.SignedOut -> signedOut()
            AppPhase.Onboarding -> OnboardingPlaceholder(onSignOut)
            AppPhase.SignedIn -> signedIn()
            is AppPhase.Failed -> FailedScreen(shown.error, onRetry, onSignOut)
        }
    }
}

/**
 * The key each phase's screen is saved under. A string, not the phase's class: a class hashes differently in every
 * process, so after process death nothing saved under the signed-in screen would be found again.
 */
internal fun AppPhase.stateKey(): String =
    when (this) {
        AppPhase.Launching -> "launching"
        AppPhase.Locked -> "locked"
        AppPhase.SignedOut -> "signed-out"
        AppPhase.Onboarding -> "onboarding"
        AppPhase.SignedIn -> "signed-in"
        is AppPhase.Failed -> "failed"
    }
