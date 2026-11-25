package com.fernandocorreia.khepri.core.model

/** The result of a repository call: a value, or the [AppError] that stopped it. */
public sealed interface Outcome<out T> {
    public data class Ok<T>(val value: T) : Outcome<T>

    public data class Err(val error: AppError) : Outcome<Nothing>
}

public inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Ok -> Outcome.Ok(transform(value))
        is Outcome.Err -> this
    }
