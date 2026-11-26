package com.fernandocorreia.khepri.feature.auth.ui.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R

/**
 * What is sent and who receives it (iOS `AIConsentDisclosure`), shared with Settings in Phase 3. The health line names
 * Health Connect where iOS names Apple Health. The rows carry a dot until the icon set arrives.
 */
@Composable
fun AiConsentDisclosure(recipients: List<String>, modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(stringResource(R.string.khepris_coach_reports_meal_and_training))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.what_is_sent), style = MaterialTheme.typography.titleMedium)
            Bullet(stringResource(R.string.what_you_write_coach_messages_check_ins))
            Bullet(stringResource(R.string.ai_consent_health_connect))
            Bullet(stringResource(R.string.about_you_your_name_age_time_zone))
            Bullet(stringResource(R.string.only_when_you_choose_to_send_or_connect))
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.who_receives_it), style = MaterialTheme.typography.titleMedium)
            recipients.forEach { Bullet(it) }
            Text(
                stringResource(R.string.if_you_connect_your_own_ai_provider_in),
                style = MaterialTheme.typography.bodySmall,
                color = KhepriTheme.colors.mutedForeground,
            )
        }
        Text(stringResource(R.string.your_data_is_used_to_answer_you_and_to))
        TextButton(onClick = { uri.openUri(PRIVACY_URL) }) { Text(stringResource(R.string.read_the_privacy_policy)) }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).background(KhepriTheme.colors.signal, CircleShape))
        Text(text)
    }
}

private const val PRIVACY_URL = "https://kheprios.com/privacy"
