package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.LocalReduceMotion
import com.fernandocorreia.khepri.core.ui.R

/** iOS `spring(response: 0.35, dampingFraction: 0.8)`: stiffness (2π / response)². */
private const val INDICATOR_DAMPING = 0.8f
private const val INDICATOR_STIFFNESS = 322f

/**
 * Sign In / Sign Up. The selected segment's card slides under the labels; with reduce motion it jumps. The indicator is
 * drawn behind the row, so it is as tall as the labels at any font scale.
 */
@Composable
internal fun ModeSwitch(mode: LoginMode, onSelect: (LoginMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = KhepriTheme.colors
    val radii = KhepriTheme.radii
    val reduceMotion = LocalReduceMotion.current
    val position by
        animateFloatAsState(
            targetValue = mode.ordinal.toFloat(),
            animationSpec = if (reduceMotion) snap() else spring(INDICATOR_DAMPING, INDICATOR_STIFFNESS),
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(radii.control))
                .background(colors.muted)
                .padding(4.dp)
                .drawBehind {
                    val width = size.width / LoginMode.entries.size
                    val corner = CornerRadius(radii.medium.toPx())
                    val topLeft = Offset(width * position, 0f)
                    val segment = Size(width, size.height)
                    drawRoundRect(colors.card, topLeft, segment, corner)
                    drawRoundRect(colors.hairline, topLeft, segment, corner, style = Stroke(1.dp.toPx()))
                }
                .selectableGroup()
    ) {
        LoginMode.entries.forEach { segment ->
            val selected = segment == mode
            Box(
                modifier =
                    Modifier.weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(radii.medium))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(segment) })
                        .testTag(if (segment == LoginMode.SignIn) "mode-sign-in" else "mode-sign-up"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (segment == LoginMode.SignIn) R.string.sign_in else R.string.sign_up),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) colors.foreground else colors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                )
            }
        }
    }
}
