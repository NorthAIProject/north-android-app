package com.fernandocorreia.khepri.core.common

import java.io.IOException

/** A token exists but cannot be read right now (Keystore failure). The request is not sent; nobody is signed out. */
public class TokenUnreadableException(cause: Throwable? = null) : IOException(cause)
