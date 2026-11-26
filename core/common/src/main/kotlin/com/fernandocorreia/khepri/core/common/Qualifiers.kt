package com.fernandocorreia.khepri.core.common

import javax.inject.Qualifier

/** The server origin without `/api/v1`, provided by `:app` from its build type. */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class ApiBaseUrl

/** A scope that lives as long as the process, for work that must outlive a screen. */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class ApplicationScope

/**
 * A scope for work that outlives a screen but belongs to the signed-in account: a refresh, a multi-step write. Its jobs
 * are cancelled when the session ends and again when the next one begins, so nothing started for one account finishes
 * under the next one's token. Sign-out and retry use [ApplicationScope]; a screen's own work uses `viewModelScope`.
 * Cancelling does not clear state held in memory: account-bound state also registers a `SignOutCleaner`.
 */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class AccountScope

/**
 * The JSON configuration the API uses (`ApiJson.default`), provided by `:app`. Modules that may not see `:core:api`,
 * such as the snapshot cache, encode and decode server shapes with it.
 */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class WireJson
