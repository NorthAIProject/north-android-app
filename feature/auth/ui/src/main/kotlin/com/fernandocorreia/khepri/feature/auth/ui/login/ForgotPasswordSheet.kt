package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.component.KhepriTextField
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.core.ui.description
import kotlinx.coroutines.launch

/** iOS `ForgotPasswordSheet`: starts from the login email; closes itself once the link is sent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForgotPasswordSheet(state: ForgotPasswordState, onEvent: (LoginEvent) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = { onEvent(LoginEvent.ForgotPasswordDismissed) },
        sheetState = sheetState,
        // A sheet is its own window: the activity's resource-id switch does not reach it.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = KhepriTheme.colors.background,
    ) {
        ForgotPasswordForm(
            state = state,
            onEvent = { event ->
                if (event == LoginEvent.ForgotPasswordDismissed) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onEvent(event) }
                } else {
                    onEvent(event)
                }
            },
        )
    }
}

@Composable
internal fun ForgotPasswordForm(
    state: ForgotPasswordState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TextButton(
            onClick = { onEvent(LoginEvent.ForgotPasswordDismissed) },
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.Start),
        ) {
            Text(stringResource(R.string.cancel))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.reset_password),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = KhepriTheme.colors.foreground,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.enter_your_email_address_to_receive),
                style = MaterialTheme.typography.bodyMedium,
                color = KhepriTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        KhepriTextField(
            value = state.email,
            onValueChange = { onEvent(LoginEvent.ResetEmailChanged(it)) },
            label = stringResource(R.string.email_address),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Send,
                    autoCorrectEnabled = false,
                ),
            keyboardActions = KeyboardActions(onSend = { onEvent(LoginEvent.ResetRequested) }),
            error = state.error?.description(),
            modifier = Modifier.semantics { contentType = ContentType.EmailAddress }.testTag("reset-email"),
        )
        ProminentButton(
            text = stringResource(R.string.send_reset_link),
            onClick = { onEvent(LoginEvent.ResetRequested) },
            enabled = state.canSend,
            loading = state.sending,
            modifier = Modifier.testTag("reset-submit"),
        )
    }
}
