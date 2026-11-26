package com.fernandocorreia.khepri.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.fernandocorreia.khepri.core.designsystem.component.KhepriEmblem
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.core.ui.description

/** A session is stored but this phone cannot read it until it is unlocked. Never the login form. */
@Composable
internal fun LockedScreen(modifier: Modifier = Modifier) {
    PhaseColumn(modifier.testTag("phase-locked")) {
        KhepriEmblem(size = KhepriTheme.spacing.xxl * 3)
        Text(
            text = stringResource(R.string.unlock_your_phone_to_continue),
            style = MaterialTheme.typography.titleMedium,
            color = KhepriTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
    }
}

/** iOS's failed phase: signed in, but the account did not load. */
@Composable
internal fun FailedScreen(error: AppError, onRetry: () -> Unit, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    PhaseColumn(modifier.testTag("phase-failed")) {
        Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = KhepriTheme.colors.mutedForeground)
        Text(
            text = stringResource(R.string.could_not_load_your_account),
            style = MaterialTheme.typography.titleLarge,
            color = KhepriTheme.colors.foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = error.description(),
            style = MaterialTheme.typography.bodyMedium,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        ProminentButton(text = stringResource(R.string.error_try_again), onClick = onRetry)
        TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out), color = KhepriTheme.colors.destructive)
        }
    }
}

/** Until onboarding is ported (Phase 3): the way out iOS's wizard also offers. */
@Composable
internal fun OnboardingPlaceholder(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    PhaseColumn(modifier.testTag("phase-onboarding")) {
        KhepriEmblem(size = KhepriTheme.spacing.xxl * 3)
        Text(
            text = stringResource(R.string.placeholder_body),
            style = MaterialTheme.typography.bodyMedium,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out), color = KhepriTheme.colors.destructive)
        }
    }
}

@Composable
private fun PhaseColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(KhepriTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(KhepriTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Preview
@Composable
private fun FailedPreview() {
    KhepriTheme { FailedScreen(AppError.Network(interrupted = false), onRetry = {}, onSignOut = {}) }
}

@Preview
@Composable
private fun LockedPreview() {
    KhepriTheme { LockedScreen() }
}
