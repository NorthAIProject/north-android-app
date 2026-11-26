package com.fernandocorreia.khepri.feature.today.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.component.Eyebrow
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.component.SurfaceCard
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.feature.today.domain.model.NextStep

/** iOS `NextStepCard`: the server's one suggestion, with the screen's one prominent button. */
@Composable
fun NextStepCard(step: NextStep, onAction: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier.testTag("next-step")) {
        Eyebrow(step.eyebrow, color = KhepriTheme.colors.signal)
        Spacer(Modifier.height(8.dp))
        Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(step.body, style = MaterialTheme.typography.bodyMedium, color = KhepriTheme.colors.mutedForeground)
        Spacer(Modifier.height(14.dp))
        ProminentButton(step.cta, onClick = onAction, modifier = Modifier.testTag("next-step-action"))
    }
}

@Preview
@Composable
private fun NextStepCardPreview() {
    KhepriTheme {
        NextStepCard(
            NextStep("workout", "Next step", "Upper body, 45 min", "Push day.", "Start workout", "/app/training"),
            onAction = {},
        )
    }
}
