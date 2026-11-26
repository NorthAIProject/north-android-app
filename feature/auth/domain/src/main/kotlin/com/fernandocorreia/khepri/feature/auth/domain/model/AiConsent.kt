package com.fernandocorreia.khepri.feature.auth.domain.model

/** iOS `AIConsentCenter.Choice`. */
enum class ConsentChoice {
    /** Not loaded yet, or the server sent a status this build does not know. */
    Unknown,
    Unanswered,
    Allowed,
    Declined;

    companion object {
        /** The server's open-string status: `unset`, `granted` or `declined`. */
        fun of(status: String): ConsentChoice =
            when (status) {
                "unset" -> Unanswered
                "granted" -> Allowed
                "declined" -> Declined
                else -> Unknown
            }
    }
}

/** Whether this account allows its data to go to third-party AI providers, and who would receive it. */
data class AiConsent(val choice: ConsentChoice, val recipients: List<String>) {
    /** The first-run question is shown only for an unanswered account. */
    val needsAnswer: Boolean
        get() = choice == ConsentChoice.Unanswered

    companion object {
        val Unknown: AiConsent = AiConsent(ConsentChoice.Unknown, emptyList())
    }
}
