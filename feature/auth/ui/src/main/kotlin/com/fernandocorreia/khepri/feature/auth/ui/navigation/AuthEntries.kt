package com.fernandocorreia.khepri.feature.auth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fernandocorreia.khepri.feature.auth.ui.login.LoginScreen

/**
 * What the shell shows while signed out. It is not a tab route: the phase root swaps it for the tabs when the session
 * gateway reports a sign-in.
 */
@Composable
fun SignedOutRoot(modifier: Modifier = Modifier) {
    LoginScreen(modifier = modifier)
}
