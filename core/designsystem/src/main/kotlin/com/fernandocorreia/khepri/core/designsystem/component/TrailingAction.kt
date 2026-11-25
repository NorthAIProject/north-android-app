package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/** A button at the end of a field (clear, show password). [contentDescription] is required: it is the only label. */
@Immutable
public data class TrailingAction(
    public val icon: Painter,
    public val contentDescription: String,
    public val onClick: () -> Unit,
)
