package com.fernandocorreia.khepri.core.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** The haptics iOS uses, by meaning. */
public enum class Haptic {
    Success,
    Selection,
    Warning,
    Error,
    Heavy,
}

/** Plays [haptic] with the closest system constant; older phones fall back to a plain click or long press. */
public fun View.perform(haptic: Haptic) {
    val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val constant =
        when (haptic) {
            Haptic.Success -> if (modern) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            Haptic.Selection -> HapticFeedbackConstants.CLOCK_TICK
            Haptic.Warning,
            Haptic.Error -> if (modern) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            Haptic.Heavy -> HapticFeedbackConstants.LONG_PRESS
        }
    performHapticFeedback(constant)
}
