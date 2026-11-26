package com.fernandocorreia.khepri.core.common

import com.fernandocorreia.khepri.core.model.AppError
import java.time.Instant

/**
 * A value shown from the cache or a fresh fetch. [refreshError] is set when the last refresh failed: the screen keeps
 * the value and shows a staleness line with [fetchedAt].
 */
public data class Cached<T>(val value: T, val fetchedAt: Instant, val refreshError: AppError? = null)
