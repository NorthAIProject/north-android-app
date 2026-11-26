package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.core.ui.description

private const val TINT_ALPHA = 0.1f

/** An error or a confirmation above the form, read out when it appears. */
@Composable
internal fun Banner(banner: LoginBanner, modifier: Modifier = Modifier) {
    val colors = KhepriTheme.colors
    val (text, tint) =
        when (banner) {
            is LoginBanner.Failed -> banner.error.description() to colors.destructive
            LoginBanner.ResetSent -> stringResource(R.string.if_that_email_is_registered_password) to colors.signal
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = colors.foreground,
        modifier =
            modifier
                .fillMaxWidth()
                .background(tint.copy(alpha = TINT_ALPHA), RoundedCornerShape(KhepriTheme.radii.medium))
                .padding(12.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("login-banner"),
    )
}
