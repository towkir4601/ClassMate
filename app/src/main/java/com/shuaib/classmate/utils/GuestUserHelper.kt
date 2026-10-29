package com.shuaib.classmate.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.shuaib.classmate.R

object GuestUserHelper {

    /**
     * Checks if the active session is anonymous / guest:
     * - Either AppPreferences has isGuestMode() == true
     * - Or FirebaseAuth.getInstance().currentUser == null
     * - Or currentUser.isAnonymous == true
     */
    fun isGuest(context: Context): Boolean {
        if (AppPreferences(context).isGuestMode()) return true
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        return firebaseUser == null || firebaseUser.isAnonymous
    }

    /**
     * Displays a friendly dialog explaining that this action requires a signed-in university account,
     * with an option to navigate to the Login screen.
     */
    fun promptLogin(context: Context, actionDescription: String = "perform this action") {
        MaterialAlertDialogBuilder(context, R.style.Theme_ClassMate_Dialog)
            .setTitle("Account Required")
            .setMessage("You are currently browsing as a guest. Please sign in with your GSTU account to $actionDescription.")
            .setPositiveButton("Sign In") { _, _ ->
                AppPreferences(context).setGuestMode(false)
                FirebaseAuth.getInstance().signOut()
                val intent = android.content.Intent(context, com.shuaib.classmate.activities.LoginActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

