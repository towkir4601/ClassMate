package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.shuaib.classmate.R
import com.shuaib.classmate.ui.SplashScreen
import com.shuaib.classmate.utils.AppPreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false
    private var pendingDestination: Class<*>? = null
    private var minLoopCompleted = false
    private lateinit var splashScreenComponent: SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        splashScreenComponent = findViewById(R.id.splashScreenComponent)

        // Listen for when 1 full loop (4 seconds) finishes
        splashScreenComponent.setOnLoopListener { loopCount ->
            if (loopCount >= 1) {
                minLoopCompleted = true
                tryNavigateIfReady()
            }
        }

        // Auto dismiss callback
        splashScreenComponent.setOnDismissListener {
            minLoopCompleted = true
            tryNavigateIfReady()
        }

        // Start 4-second animation
        splashScreenComponent.play()

        // Concurrently check user status in the background
        checkUserStatus()
    }

    private fun checkUserStatus() {
        val auth = FirebaseAuth.getInstance()
        val prefs = AppPreferences(this)
        val currentUser = auth.currentUser

        if (!prefs.isOnboardingComplete()) {
            setResolvedDestination(OnboardingActivity::class.java)
        } else if (currentUser != null) {
            fetchProfileAndRoute(currentUser.uid)
        } else {
            setResolvedDestination(LoginActivity::class.java)
        }
    }

    private fun fetchProfileAndRoute(uid: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        val timeoutRunnable = Runnable {
            if (pendingDestination == null) {
                setResolvedDestination(MainActivity::class.java)
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        userRef.get(Source.CACHE)
            .addOnSuccessListener { cached ->
                if (cached.exists()) {
                    handler.removeCallbacks(timeoutRunnable)
                    routeFromProfile(cached)
                } else {
                    fetchFromServer(userRef, timeoutRunnable)
                }
            }
            .addOnFailureListener {
                fetchFromServer(userRef, timeoutRunnable)
            }
    }

    private fun fetchFromServer(userRef: com.google.firebase.firestore.DocumentReference, timeoutRunnable: Runnable) {
        userRef.get(Source.SERVER)
            .addOnSuccessListener { doc ->
                handler.removeCallbacks(timeoutRunnable)
                if (doc.exists()) {
                    routeFromProfile(doc)
                } else {
                    setResolvedDestination(MainActivity::class.java)
                }
            }
            .addOnFailureListener {
                handler.removeCallbacks(timeoutRunnable)
                setResolvedDestination(MainActivity::class.java)
            }
    }

    private fun routeFromProfile(document: DocumentSnapshot) {
        try {
            val role = document.getString("role") ?: "student"
            val approvedVal = document.get("approved")
            val approved = when (approvedVal) {
                is Boolean -> approvedVal
                is String -> approvedVal.toBoolean()
                else -> false
            }
            
            if (role == "superadmin" || role == "admin" || approved) {
                setResolvedDestination(MainActivity::class.java)
            } else {
                setResolvedDestination(PendingApprovalActivity::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error parsing profile: ${e.message}")
            setResolvedDestination(MainActivity::class.java)
        }
    }

    private fun setResolvedDestination(destination: Class<*>) {
        pendingDestination = destination
        tryNavigateIfReady()
    }

    private fun tryNavigateIfReady() {
        val destination = pendingDestination ?: return
        if (minLoopCompleted && !hasNavigated && !isFinishing) {
            hasNavigated = true
            startActivity(Intent(this, destination))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onPause() {
        super.onPause()
        splashScreenComponent.pause()
    }

    override fun onResume() {
        super.onResume()
        splashScreenComponent.resume()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
