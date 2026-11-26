package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.component.KhepriEmblem
import com.fernandocorreia.khepri.core.designsystem.component.KhepriTextField
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R

@Composable
internal fun SignInForm(state: LoginUiState, onEvent: (LoginEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EmailField(state.email, onEvent)
        PasswordField(
            value = state.password,
            label = stringResource(R.string.password),
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            contentType = ContentType.Password,
            last = true,
            onDone = { onEvent(LoginEvent.Submit) },
            tag = "login-password",
        )
        TextButton(
            onClick = { onEvent(LoginEvent.ForgotPasswordOpened) },
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End).testTag("forgot-password"),
        ) {
            Text(stringResource(R.string.forgot_password), fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
        }
        ProminentButton(
            text = stringResource(R.string.sign_in),
            onClick = { onEvent(LoginEvent.Submit) },
            enabled = state.canSubmit,
            loading = state.running,
            modifier = Modifier.testTag("login-submit"),
        )
    }
}

@Composable
internal fun SignUpForm(state: LoginUiState, onEvent: (LoginEvent) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        KhepriTextField(
            value = state.name,
            onValueChange = { onEvent(LoginEvent.NameChanged(it)) },
            label = stringResource(R.string.your_name),
            keyboardOptions =
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.semantics { contentType = ContentType.PersonFullName }.testTag("signup-name"),
        )
        EmailField(state.email, onEvent)
        PasswordField(
            value = state.password,
            label = stringResource(R.string.password_min_8_characters),
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            contentType = ContentType.NewPassword,
            tag = "login-password",
        )
        if (state.password.isNotEmpty()) PasswordStrengthMeter(state.strength)
        PasswordField(
            value = state.confirmation,
            label = stringResource(R.string.confirm_password),
            onValueChange = { onEvent(LoginEvent.ConfirmationChanged(it)) },
            contentType = ContentType.NewPassword,
            last = true,
            onDone = { onEvent(LoginEvent.Submit) },
            error = if (state.passwordsDiffer) stringResource(R.string.passwords_do_not_match) else null,
            tag = "signup-confirmation",
        )
        ProminentButton(
            text = stringResource(R.string.create_account),
            onClick = { onEvent(LoginEvent.Submit) },
            enabled = state.canSubmit,
            loading = state.running,
            modifier = Modifier.testTag("login-submit"),
        )
    }
}

@Composable
internal fun EmailField(value: String, onEvent: (LoginEvent) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    KhepriTextField(
        value = value,
        onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
        label = stringResource(R.string.email_address),
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
        modifier = modifier.semantics { contentType = ContentType.EmailAddress }.testTag("login-email"),
    )
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    contentType: ContentType,
    tag: String,
    modifier: Modifier = Modifier,
    last: Boolean = false,
    onDone: () -> Unit = {},
    error: String? = null,
) {
    val focus = LocalFocusManager.current
    KhepriTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        error = error,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (last) ImeAction.Go else ImeAction.Next,
                autoCorrectEnabled = false,
            ),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }, onGo = { onDone() }),
        modifier = modifier.semantics { this.contentType = contentType }.testTag(tag),
    )
}

/** The brand header: emblem, wordmark in Marcellus, tagline. */
@Composable
internal fun BrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KhepriEmblem(
            size = 120.dp,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.brand_name),
            style = KhepriTheme.textStyles.displayBrand,
            color = KhepriTheme.colors.foreground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.brand_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
