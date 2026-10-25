package com.shuaib.classmate.utils

import android.util.Log
import com.shuaib.classmate.BuildConfig

/**
 * Production-safe logging utility for GSTU Go (ClassMate).
 * Automatically strips and suppresses debug and verbose log messages in release builds
 * to prevent PII, auth tokens, or internal state leakage (OWASP M6).
 */
object AppLogger {

    inline fun d(tag: String, messageSupplier: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, messageSupplier())
        }
    }

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
        }
    }
}
