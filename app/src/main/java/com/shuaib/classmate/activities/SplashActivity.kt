package com.shuaib.classmate.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.shuaib.classmate.databinding.ActivitySplashBinding
import com.shuaib.classmate.utils.AppPreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var hasNavigated = false
    private var startTime = 0L

    companion object {
        private const val MIN_SPLASH_DURATION_MS = 1800L
        private const val MAX_TIMEOUT_MS = 3500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12 SplashScreen support and immediately dismiss the system splash view
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }
        super.onCreate(savedInstanceState)

        // Make window immersive edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startTime = System.currentTimeMillis()

        // Prepare and start entrance animations
        startEntranceAnimations()

        // Check authentication and user destination in parallel
        checkUserStatus()
    }

    /**
     * Staggered, choreographed entrance animation:
     * 1. Campus building background fades in with subtle zoom
     * 2. Vignette gradient smoothly covers background
     * 3. Header university title descends into view
     * 4. HD GSTU GO logo scales in with custom overshoot bounce
     * 5. Halo aura glows around the logo
     * 6. App title & Bengali badge slide up into position
     * 7. Tagline & indeterminate progress bar appear gracefully at the footer
     */
    private fun startEntranceAnimations() {
        // Initial setup for animated views
        binding.ivCampusBg.scaleX = 1.08f
        binding.ivCampusBg.scaleY = 1.08f
        binding.ivCampusBg.alpha = 0f

        binding.vVignetteOverlay.alpha = 0f

        binding.tvUniversityName.alpha = 0f
        binding.tvUniversityName.translationY = -40f

        binding.flLogoContainer.alpha = 0f
        binding.flLogoContainer.scaleX = 0.5f
        binding.flLogoContainer.scaleY = 0.5f

        binding.ivLogoGlow.alpha = 0f

        binding.tvAppTitle.alpha = 0f
        binding.tvAppTitle.translationY = 50f

        binding.tvAppSubtitleBn.alpha = 0f
        binding.tvAppSubtitleBn.translationY = 30f

        binding.llFooter.alpha = 0f

        // 1. Campus background fade & settle
        binding.ivCampusBg.animate()
            .alpha(0.85f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(1200L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 2. Vignette dark overlay
        binding.vVignetteOverlay.animate()
            .alpha(1f)
            .setDuration(800L)
            .start()

        // 3. Header title descends
        binding.tvUniversityName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600L)
            .setStartDelay(50L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4. Center HD Logo scales in with elegant overshoot right from the start
        binding.flLogoContainer.animate()
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(750L)
            .setStartDelay(80L)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()

        // 5. Logo halo glow fades in
        binding.ivLogoGlow.animate()
            .alpha(0.7f)
            .setDuration(800L)
            .setStartDelay(180L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 6. GSTU Go brand title slides up
        binding.tvAppTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(550L)
            .setStartDelay(260L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 7. Bengali subtitle slides up
        binding.tvAppSubtitleBn.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500L)
            .setStartDelay(380L)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 8. Footer tagline and loader fade in
        binding.llFooter.animate()
            .alpha(1f)
            .setDuration(450L)
            .setStartDelay(500L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun checkUserStatus() {
        val auth = FirebaseAuth.getInstance()
        val prefs = AppPreferences(this)
        val currentUser = auth.currentUser

        if (!prefs.isOnboardingComplete()) {
            scheduleNavigation(OnboardingActivity::class.java)
        } else if (prefs.isGuestMode()) {
            scheduleNavigation(MainActivity::class.java)
        } else if (currentUser != null) {
            fetchProfileAndRoute(currentUser.uid)
        } else {
            scheduleNavigation(LoginActivity::class.java)
        }
    }

    private fun fetchProfileAndRoute(uid: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        val timeoutRunnable = Runnable {
            if (!hasNavigated) {
                scheduleNavigation(MainActivity::class.java)
            }
        }
        handler.postDelayed(timeoutRunnable, MAX_TIMEOUT_MS)

        // Try cache first for instant resolution
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
                    scheduleNavigation(MainActivity::class.java)
                }
            }
            .addOnFailureListener {
                handler.removeCallbacks(timeoutRunnable)
                scheduleNavigation(MainActivity::class.java)
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
                scheduleNavigation(MainActivity::class.java)
            } else {
                scheduleNavigation(PendingApprovalActivity::class.java)
            }
        } catch (e: Exception) {
            android.util.Log.e("SplashActivity", "Error parsing profile: ${e.message}")
            scheduleNavigation(MainActivity::class.java)
        }
    }

    /**
     * Ensures the splash animation is shown for at least MIN_SPLASH_DURATION_MS
     * so that the user experiences the full cinematic opening without abrupt jumps.
     */
    private fun scheduleNavigation(destination: Class<*>) {
        val elapsed = System.currentTimeMillis() - startTime
        val remainingDelay = maxOf(0L, MIN_SPLASH_DURATION_MS - elapsed)

        handler.postDelayed({
            navigate(destination)
        }, remainingDelay)
    }

    private fun navigate(destination: Class<*>) {
        if (hasNavigated || isFinishing) return
        hasNavigated = true

        val intent = Intent(this, destination)
        startActivity(intent)
        finish()
        // Seamless crossfade transition into the main app
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
