package com.fernandocorreia.khepri.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import com.fernandocorreia.khepri.core.model.AppError

/**
 * What a banner or inline error says for [this], as iOS `APIError.errorDescription`: the server's message where it sent
 * one, otherwise iOS's fallback copy. A dropped connection uses the offline copy, where iOS shows the system's text.
 */
@Composable @ReadOnlyComposable public fun AppError.description(): String = description(LocalResources.current)

/** [description] outside composition. */
public fun AppError.description(resources: Resources): String {
    fun text(id: Int) = resources.getString(id)
    fun String?.or(id: Int) = this?.takeIf { it.isNotBlank() } ?: text(id)
    return when (this) {
        is AppError.Unauthorized -> message.or(R.string.your_session_has_expired_please_sign_in)
        is AppError.NotFound -> message.or(R.string.that_could_not_be_found)
        is AppError.Conflict -> message.or(R.string.that_is_already_in_progress)
        is AppError.FieldValidation -> message.or(R.string.error_generic_body)
        is AppError.Server ->
            message?.takeIf { it.isNotBlank() }
                ?: resources.getString(R.string.request_failed_with_status_code_x, status)
        is AppError.Refused -> message.or(R.string.error_generic_body)
        is AppError.Network -> text(R.string.error_offline_body)
        AppError.Locked -> text(R.string.unlock_your_phone_to_continue)
        AppError.InvalidResponse -> text(R.string.invalid_server_response)
        AppError.SignedOut -> text(R.string.error_generic_body)
    }
}
