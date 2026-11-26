package com.fernandocorreia.khepri.core.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** iOS `APIError.errorDescription`, case by case. */
@RunWith(AndroidJUnit4::class)
class ErrorTextTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    private fun text(error: AppError) = error.description(resources)

    @Test
    fun `the server's message wins wherever iOS shows it`() {
        assertEquals("expired", text(AppError.Unauthorized("expired")))
        assertEquals("Email is taken", text(AppError.FieldValidation("Email is taken", mapOf("email" to "taken"))))
        assertEquals("Too many attempts", text(AppError.Server(429, "Too many attempts", null)))
        assertEquals("Import failed", text(AppError.Refused("import_partial", "Import failed")))
    }

    @Test
    fun `without a message each case has iOS's copy`() {
        assertEquals("Your session has expired. Please sign in again.", text(AppError.Unauthorized(null)))
        assertEquals("That could not be found.", text(AppError.NotFound(" ")))
        assertEquals("That is already in progress.", text(AppError.Conflict(null, null)))
        assertEquals("Request failed with status code 503.", text(AppError.Server(503, null, null)))
        assertEquals("Unlock your phone to continue.", text(AppError.Locked))
        assertEquals("Invalid server response.", text(AppError.InvalidResponse))
        assertEquals("Check your connection and try again.", text(AppError.Network(interrupted = true)))
    }
}
