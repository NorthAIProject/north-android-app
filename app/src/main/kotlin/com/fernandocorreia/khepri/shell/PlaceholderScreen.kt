package com.fernandocorreia.khepri.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.Route
import com.fernandocorreia.khepri.core.ui.R

/** Any route not ported yet. The More root also offers Sign Out, with iOS's confirmation. */
@Composable
internal fun PlaceholderScreen(
    route: Route,
    modifier: Modifier = Modifier,
    viewModel: PlaceholderViewModel =
        hiltViewModel<PlaceholderViewModel, PlaceholderViewModel.Factory>(creationCallback = { it.create(route) }),
) {
    val taps by viewModel.taps.collectAsStateWithLifecycle()
    PlaceholderContent(
        route = route,
        taps = taps,
        onTap = viewModel::tap,
        onSignOut = viewModel::signOut,
        modifier = modifier,
    )
}

@Composable
internal fun PlaceholderContent(
    route: Route,
    taps: Int,
    onTap: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val tab = (route as? Route.TabRoot)?.tab
    Column(
        modifier = modifier.fillMaxSize().padding(KhepriTheme.spacing.xxl).testTag("placeholder"),
        verticalArrangement = Arrangement.spacedBy(KhepriTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (tab != null) {
            Text(
                text = stringResource(tab.label()),
                style = MaterialTheme.typography.headlineSmall,
                color = KhepriTheme.colors.foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = stringResource(R.string.placeholder_body),
            style = MaterialTheme.typography.bodyMedium,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onTap, modifier = Modifier.testTag("placeholder-taps")) { Text(taps.toString()) }
        if (tab == AppTab.More) {
            TextButton(onClick = { confirming = true }, modifier = Modifier.testTag("sign-out")) {
                Text(stringResource(R.string.sign_out), color = KhepriTheme.colors.destructive)
            }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            // A dialog is its own window: the activity's resource-id switch does not reach it.
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            title = { Text(stringResource(R.string.sign_out_of_khepri)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("sign-out-confirm"),
                ) {
                    Text(stringResource(R.string.sign_out), color = KhepriTheme.colors.destructive)
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
