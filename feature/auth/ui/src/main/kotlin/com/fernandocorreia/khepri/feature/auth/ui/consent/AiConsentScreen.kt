package com.fernandocorreia.khepri.feature.auth.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.core.ui.description

/**
 * The first-run AI data sharing question (iOS `AIConsentScreen`), over everything until answered. The host swallows
 * Back while it shows: iOS disables interactive dismissal.
 */
@Composable
fun AiConsentScreen(modifier: Modifier = Modifier, viewModel: AiConsentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AiConsentContent(state, viewModel::onEvent, modifier)
}

@Composable
internal fun AiConsentContent(
    state: AiConsentUiState,
    onEvent: (AiConsentEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(KhepriTheme.colors.background)
                .safeDrawingPadding()
                .semantics { testTagsAsResourceId = true }
                .testTag("ai-consent"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.share_your_data_with_ai),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            AiConsentDisclosure(state.recipients)
        }
        Answers(state, onEvent)
    }
}

@Composable
private fun Answers(state: AiConsentUiState, onEvent: (AiConsentEvent) -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(KhepriTheme.colors.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.error?.let {
            Text(
                it.description(),
                style = MaterialTheme.typography.bodySmall,
                color = KhepriTheme.colors.destructive,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("ai-consent-error"),
            )
        }
        ProminentButton(
            text = stringResource(R.string.allow),
            onClick = { onEvent(AiConsentEvent.Allow) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("ai-consent-allow"),
        )
        OutlinedButton(
            onClick = { onEvent(AiConsentEvent.Decline) },
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("ai-consent-decline"),
        ) {
            Text(stringResource(R.string.dont_allow))
        }
        Text(
            stringResource(R.string.if_you_dont_allow_this_ai_features_stay),
            style = MaterialTheme.typography.bodySmall,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun AiConsentPreview() {
    KhepriTheme { AiConsentContent(AiConsentUiState(recipients = listOf("Anthropic", "Google")), onEvent = {}) }
}

@Preview
@Composable
private fun AiConsentFailedPreview() {
    KhepriTheme {
        AiConsentContent(
            AiConsentUiState(recipients = listOf("Anthropic"), error = AppError.Server(500, "Try again", null)),
            onEvent = {},
        )
    }
}
