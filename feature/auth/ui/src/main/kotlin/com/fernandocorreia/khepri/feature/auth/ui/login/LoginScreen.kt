package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.ui.LocalReduceMotion
import com.fernandocorreia.khepri.core.ui.perform

/** The signed-out screen: email sign-in and sign-up. Passkey, Google and Apple arrive with lane F. */
@Composable
fun LoginScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginEffect.Play -> view.perform(effect.haptic)
            }
        }
    }
    LoginContent(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

@Composable
internal fun LoginContent(state: LoginUiState, onEvent: (LoginEvent) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(KhepriTheme.colors.background)
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            BrandHeader()
            ModeSwitch(state.mode, onSelect = { onEvent(LoginEvent.ModeChanged(it)) })
            state.banner?.let { Banner(it) }
            Forms(state, onEvent)
        }
    }
    state.forgotPassword?.let { ForgotPasswordSheet(it, onEvent) }
}

/** Sign-in slides in from the start, sign-up from the end, as on iOS; with reduce motion the form just changes. */
@Composable
private fun Forms(state: LoginUiState, onEvent: (LoginEvent) -> Unit) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedContent(
        targetState = state.mode,
        transitionSpec = {
            if (reduceMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val forward = targetState == LoginMode.SignUp
                (slideInHorizontally { if (forward) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it else it } + fadeOut())
            }
        },
    ) { mode ->
        when (mode) {
            LoginMode.SignIn -> SignInForm(state, onEvent)
            LoginMode.SignUp -> SignUpForm(state, onEvent)
        }
    }
}

@Preview
@Composable
private fun SignInPreview() {
    KhepriTheme { LoginContent(LoginUiState(email = "ana@example.com", password = "secret"), onEvent = {}) }
}

@Preview
@Composable
private fun SignUpPreview() {
    KhepriTheme {
        LoginContent(
            LoginUiState(
                mode = LoginMode.SignUp,
                name = "Ana",
                email = "ana@example.com",
                password = "Secret12",
                confirmation = "Secret1",
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun RunningPreview() {
    KhepriTheme {
        LoginContent(LoginUiState(email = "ana@example.com", password = "secret", running = true), onEvent = {})
    }
}

@Preview
@Composable
private fun FailedPreview() {
    KhepriTheme {
        LoginContent(
            LoginUiState(banner = LoginBanner.Failed(AppError.Unauthorized("Invalid email or password"))),
            onEvent = {},
        )
    }
}
