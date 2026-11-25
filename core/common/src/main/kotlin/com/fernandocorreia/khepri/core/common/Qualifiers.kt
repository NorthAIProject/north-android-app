package com.fernandocorreia.khepri.core.common

import javax.inject.Qualifier

/** The server origin without `/api/v1`, provided by `:app` from its build type. */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class ApiBaseUrl

/** A scope that lives as long as the process, for work that must outlive a screen. */
@Qualifier @Retention(AnnotationRetention.BINARY) public annotation class ApplicationScope
