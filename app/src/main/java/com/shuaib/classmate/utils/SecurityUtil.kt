package com.shuaib.classmate.utils

import android.os.Build
import android.os.Debug
import java.io.File

/**
 * Security and device integrity verification utilities for GSTU Go (ClassMate).
 * Detects rooted environments, test-key ROMs, and active debuggers.
 */
object SecurityUtil {

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    /**
     * Checks if the device has test-keys or standard superuser/su binaries installed.
     */
    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuPaths()
    }

    private fun checkBuildTags(): Boolean {
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    private fun checkSuPaths(): Boolean {
        return try {
            SU_PATHS.any { path ->
                try {
                    File(path).exists()
                } catch (_: Exception) {
                    false
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if an active debugger is attached to the process.
     */
    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }
}
