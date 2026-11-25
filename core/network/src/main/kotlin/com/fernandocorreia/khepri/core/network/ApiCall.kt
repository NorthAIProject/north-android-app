package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.Response

/** Runs a generated call and turns every response and failure into an [Outcome]. Nothing throws except cancellation. */
public suspend fun <T : Any> apiCall(block: suspend () -> Response<T>): Outcome<T> =
    run(block) { response -> response.body()?.let { Outcome.Ok(it) } ?: Outcome.Err(AppError.InvalidResponse) }

/** For operations that answer 2xx with no body (204). */
public suspend fun apiCallUnit(block: suspend () -> Response<Unit>): Outcome<Unit> = run(block) { Outcome.Ok(Unit) }

private suspend fun <T, R> run(block: suspend () -> Response<T>, success: (Response<T>) -> Outcome<R>): Outcome<R> =
    try {
        val response = block()
        if (response.isSuccessful) success(response)
        else Outcome.Err(errorFor(response.code(), response.errorBody()?.use { it.string() }))
    } catch (e: CancellationException) {
        throw e
    } catch (expected: TokenUnreadableException) {
        Outcome.Err(AppError.Locked)
    } catch (e: IOException) {
        Outcome.Err(AppError.Network(interrupted = e.isInterruption()))
    } catch (expected: SerializationException) {
        Outcome.Err(AppError.InvalidResponse)
    } catch (expected: IllegalArgumentException) {
        Outcome.Err(AppError.InvalidResponse)
    }

private fun IOException.isInterruption(): Boolean =
    this is SocketTimeoutException ||
        this is ConnectException ||
        this is UnknownHostException ||
        (this is SocketException && message?.contains("reset", ignoreCase = true) == true)
