package com.shuaib.classmate.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.shuaib.classmate.R
import com.shuaib.classmate.databinding.ActivityPendingApprovalBinding

class PendingApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingApprovalBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Real-time Firestore listener
    private var approvalListener: ListenerRegistration? = null

    // Email verification polling
    private val handler = Handler(Looper.getMainLooper())
    private val emailCheckRunnable = object : Runnable {
        override fun run() {
            auth.currentUser?.reload()?.addOnCompleteListener {
                if (auth.currentUser?.isEmailVerified == true) {
                    // Email verified! Move to approval waiting state
                    showApprovalWaitingState()
                    startApprovalListener()
                } else {
                    // Check again in 5 seconds
                    handler.postDelayed(this, 5000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvEmail.text = auth.currentUser?.email ?: ""

        // Determine initial state
        determineInitialState()

        binding.btnResendEmail.setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show()
            }?.addOnFailureListener {
                Toast.makeText(this, "Failed to send email: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSignOut.setOnClickListener {
            approvalListener?.remove()
            handler.removeCallbacksAndMessages(null)
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun determineInitialState() {
        val user = auth.currentUser ?: return

        if (!user.isEmailVerified) {
            showEmailVerificationState()
            // Start polling for email verification every 5 seconds
            handler.postDelayed(emailCheckRunnable, 5000)
        } else {
            showApprovalWaitingState()
            startApprovalListener()
        }
    }

    private fun showEmailVerificationState() {
        binding.tvTitle.text = "Email Verification Required"
        binding.tvMessage.text = "A verification link has been sent to your email.\nPlease check your inbox and click the link to verify."
        binding.btnResendEmail.visibility = View.VISIBLE
        binding.tvLiveStatus.text = "🔄 Auto-checking verification status..."

        // Step indicators
        binding.tvStep1Label.text = "Email Verify"
        binding.tvStep2Label.text = "Admin Approval"
        binding.ivStep1Icon.setColorFilter(ContextCompat.getColor(this, R.color.cm_warning))
        binding.ivStep2Icon.setColorFilter(ContextCompat.getColor(this, R.color.cm_text_disabled))
        binding.stepConnector.setBackgroundColor(ContextCompat.getColor(this, R.color.cm_text_disabled))

        // Show pulsing animation on status text
        startPulseAnimation(binding.tvLiveStatus)
        binding.progressWaiting.visibility = View.VISIBLE
    }

    private fun showApprovalWaitingState() {
        binding.tvTitle.text = "Waiting for Admin Approval"
        binding.tvMessage.text = "Your email has been verified! ✅\n\nPlease wait — you'll be logged in automatically as soon as an admin approves your account."
        binding.btnResendEmail.visibility = View.GONE
        binding.tvLiveStatus.text = "🔄 Listening for approval..."

        // Step indicators - step 1 done
        binding.ivStep1Icon.setColorFilter(ContextCompat.getColor(this, R.color.cm_success))
        binding.tvStep1Label.text = "✅ Verified"
        binding.stepConnector.setBackgroundColor(ContextCompat.getColor(this, R.color.cm_success))
        binding.ivStep2Icon.setColorFilter(ContextCompat.getColor(this, R.color.cm_warning))
        binding.tvStep2Label.text = "⏳ Approval"

        // Show pulsing animation
        startPulseAnimation(binding.tvLiveStatus)
        binding.progressWaiting.visibility = View.VISIBLE
    }

    // 🔥 Real-time Firestore listener - catches approval instantly!
    private fun startApprovalListener() {
        val uid = auth.currentUser?.uid ?: return
        approvalListener?.remove()

        approvalListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val approvedVal = snapshot.get("approved")
                val approved = when (approvedVal) {
                    is Boolean -> approvedVal
                    is String -> approvedVal.toBoolean()
                    else -> false
                }

                if (approved) {
                    // 🎉 Admin approved! Navigate to Main Screen!
                    approvalListener?.remove()
                    handler.removeCallbacksAndMessages(null)

                    binding.progressWaiting.visibility = View.GONE
                    binding.tvLiveStatus.clearAnimation()
                    binding.tvLiveStatus.text = "✅ Approved! Logging you in..."

                    // Step indicators - both done
                    binding.ivStep2Icon.setColorFilter(ContextCompat.getColor(this, R.color.cm_success))
                    binding.tvStep2Label.text = "✅ Approved"

                    // Short delay for visual feedback then navigate
                    handler.postDelayed({
                        navigateToMain()
                    }, 1200)
                }
            }
    }

    private fun navigateToMain() {
        if (isFinishing || isDestroyed) return
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finishAffinity()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun startPulseAnimation(view: View) {
        val pulse = AlphaAnimation(1f, 0.4f).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        view.startAnimation(pulse)
    }

    override fun onResume() {
        super.onResume()
        // Also check on resume as a fallback
        auth.currentUser?.reload()?.addOnCompleteListener {
            val user = auth.currentUser ?: return@addOnCompleteListener
            if (user.isEmailVerified && approvalListener == null) {
                showApprovalWaitingState()
                startApprovalListener()
            } else if (!user.isEmailVerified) {
                // Make sure polling is running
                handler.removeCallbacks(emailCheckRunnable)
                handler.postDelayed(emailCheckRunnable, 5000)
            }
        }
    }

    override fun onDestroy() {
        approvalListener?.remove()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
