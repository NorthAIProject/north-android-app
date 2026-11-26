package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.ui.LocalReduceMotion
import com.fernandocorreia.khepri.core.ui.R
import com.fernandocorreia.khepri.feature.auth.domain.model.PasswordStrength

private const val BARS = 4
private const val BAR_ANIMATION_MS = 200

/** Four bars and the strength's name. The bars carry the colour; the word stays in the text colour, as on iOS. */
@Composable
internal fun PasswordStrengthMeter(strength: PasswordStrength, modifier: Modifier = Modifier) {
    val colors = KhepriTheme.colors
    val score = strength.ordinal
    val fill =
        when (strength) {
            PasswordStrength.VeryWeak,
            PasswordStrength.Weak -> colors.destructive
            PasswordStrength.Fair -> colors.ember
            PasswordStrength.Good,
            PasswordStrength.Strong -> colors.signal
        }
    val reduceMotion = LocalReduceMotion.current
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(BARS) { index ->
                val color by
                    animateColorAsState(
                        targetValue = if (index < score) fill else colors.muted,
                        animationSpec = if (reduceMotion) snap() else tween(BAR_ANIMATION_MS),
                    )
                Box(Modifier.weight(1f).height(4.dp).background(color, CircleShape))
            }
        }
        val label = stringResource(R.string.password_strength)
        val name = stringResource(strength.title())
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.mutedForeground)) { append(label) }
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(name) }
                },
            style = MaterialTheme.typography.labelSmall,
            color = colors.foreground,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private fun PasswordStrength.title(): Int =
    when (this) {
        PasswordStrength.VeryWeak -> R.string.very_weak
        PasswordStrength.Weak -> R.string.weak
        PasswordStrength.Fair -> R.string.fair
        PasswordStrength.Good -> R.string.good
        PasswordStrength.Strong -> R.string.strong
    }
