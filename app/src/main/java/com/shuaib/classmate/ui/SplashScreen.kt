package com.shuaib.classmate.ui

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.shuaib.classmate.R

/**
 * Reusable SplashScreen component for ClassMate.
 * 
 * Displays the sunset loading screen with background photo (bg.jpg) and Lottie vector animation
 * (sunset_loading_screen.json). Loops the 4-second animation and auto-dismisses after completion of one loop.
 */
class SplashScreen @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SplashScreen"
        // The optimized Lottie JSON — references bg_1080x1920.webp as its background layer
        const val DEFAULT_ANIMATION_ASSET   = "Loading_Screen_fixed.json"
        const val FALLBACK_ANIMATION_ASSET  = "loading_screen.json"
        const val BACKGROUND_IMAGE_NAME     = "bg_1080x1920.webp"
        const val ANIMATION_LOOP_DURATION_MS = 4000L // 120 frames @ 30 fps
        private const val MIN_PLAY_TIME_FOR_LOOP_MS = 3500L
    }

    // Direct background image view for instant photo display with zero latency
    private val backgroundImageView = ImageView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    // Lottie animation view overlays the vector animation on top of the background.
    // CENTER_CROP: scales the 1080×1920 composition to cover the full screen (same
    // factor as backgroundImageView), so vector elements stay aligned to the background.
    val lottieView: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.lottieSplash
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private val handler = Handler(Looper.getMainLooper())
    private var onDismissListener: (() -> Unit)? = null
    private var onLoopListener: ((loopCount: Int) -> Unit)? = null
    private var isDismissed = false
    private var playStartTime: Long = 0L
    private var cachedBackgroundBitmap: Bitmap? = null

    var autoDismissAfterOneLoop: Boolean = true
    var dismissFadeDuration: Long = 350L

    init {
        setBackgroundColor(Color.parseColor("#0F172A"))

        // Add background photo first, then Lottie animation on top
        addView(backgroundImageView)
        addView(lottieView)

        loadBackgroundPhoto()
        setupLottieAssets()
        setupAnimatorListeners()
    }

    /**
     * Loads the background photo (bg.jpg) directly into the ImageView for instant visibility.
     */
    private fun loadBackgroundPhoto() {
        val bitmap = getBackgroundBitmap()
        if (bitmap != null) {
            backgroundImageView.setImageBitmap(bitmap)
            Log.d(TAG, "Background photo '$BACKGROUND_IMAGE_NAME' loaded successfully (${bitmap.width}x${bitmap.height})")
        } else {
            Log.e(TAG, "Failed to load background photo '$BACKGROUND_IMAGE_NAME' from assets")
        }
    }

    /**
     * Returns cached bitmap of bg.jpg or decodes from assets.
     */
    private fun getBackgroundBitmap(): Bitmap? {
        if (cachedBackgroundBitmap == null || cachedBackgroundBitmap?.isRecycled == true) {
            cachedBackgroundBitmap = loadBitmapFromAssets(BACKGROUND_IMAGE_NAME)
        }
        return cachedBackgroundBitmap
    }

    /**
     * Decodes a bitmap from the assets directory.
     */
    private fun loadBitmapFromAssets(fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            try {
                context.assets.open("assets/$fileName").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open '$fileName' from assets", e)
                null
            }
        }
    }

    /**
     * Set up image asset folder and image delegate for Lottie.
     */
    private fun setupLottieAssets() {
        // Do NOT set imageAssetsFolder — the delegate handles all image resolution.
        // Setting imageAssetsFolder = "" pointed Lottie at an invalid path and caused silent failures.
        // The dedicated backgroundImageView behind the LottieView already shows bg.jpg correctly
        // scaled via CENTER_CROP. Returning the raw 720×1280 bg.jpg caused Lottie to tile it
        // in 3 columns to fill its 1080×1920 canvas. Transparent lets the ImageView show through
        // while Lottie renders only its vector layers (sun shimmer, birds, spinner, text, dots).
        lottieView.setImageAssetDelegate { asset ->
            val fileName = asset.fileName ?: BACKGROUND_IMAGE_NAME
            val bitmap = loadBitmapFromAssets(fileName) ?: getBackgroundBitmap()
            if (bitmap != null) {
                Log.d(TAG, "Lottie image asset resolved: id=${asset.id}, file=$fileName (${bitmap.width}x${bitmap.height})")
                bitmap
            } else {
                val w = if (asset.width > 0) asset.width else 1080
                val h = if (asset.height > 0) asset.height else 1920
                Log.w(TAG, "Lottie image asset fallback to empty bitmap: id=${asset.id}, file=$fileName")
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            }
        }

        lottieView.setIgnoreDisabledSystemAnimations(true)
        lottieView.repeatCount = LottieDrawable.INFINITE

        lottieView.setFailureListener { throwable ->
            Log.e(TAG, "Lottie animation failure", throwable)
        }

        lottieView.addLottieOnCompositionLoadedListener { composition ->
            Log.d(TAG, "Lottie composition loaded: duration=${composition.duration}ms, frames=${composition.endFrame}")
            if (playStartTime > 0 && !isDismissed) {
                lottieView.playAnimation()
            }
        }

        // Load the sunset loading animation asset
        loadAnimationAsset(DEFAULT_ANIMATION_ASSET)
    }

    /**
     * Loads the Lottie JSON animation from assets.
     */
    fun loadAnimationAsset(assetName: String) {
        try {
            lottieView.setAnimation(assetName)
            Log.d(TAG, "Set animation asset: $assetName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load animation asset '$assetName', trying fallback", e)
            try {
                lottieView.setAnimation(FALLBACK_ANIMATION_ASSET)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Failed to load fallback animation asset '$FALLBACK_ANIMATION_ASSET'", fallbackError)
            }
        }
    }

    /**
     * Attaches animator listeners for loop counting.
     */
    private fun setupAnimatorListeners() {
        lottieView.repeatCount = LottieDrawable.INFINITE

        lottieView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                Log.d(TAG, "Lottie animation started playing")
            }

            override fun onAnimationEnd(animation: Animator) {
                val elapsed = SystemClock.uptimeMillis() - playStartTime
                Log.d(TAG, "Lottie onAnimationEnd called (elapsed=${elapsed}ms)")
                // Ignore spurious onAnimationEnd calls on initialization before the animation actually ran
                if (playStartTime > 0 && elapsed >= MIN_PLAY_TIME_FOR_LOOP_MS) {
                    handleLoopFinished()
                }
            }

            override fun onAnimationCancel(animation: Animator) {
                Log.d(TAG, "Lottie animation cancelled")
            }

            override fun onAnimationRepeat(animation: Animator) {
                val elapsed = SystemClock.uptimeMillis() - playStartTime
                Log.d(TAG, "Lottie onAnimationRepeat called (elapsed=${elapsed}ms)")
                if (playStartTime > 0 && elapsed >= MIN_PLAY_TIME_FOR_LOOP_MS) {
                    handleLoopFinished()
                }
            }
        })
    }

    /**
     * Triggered when 1 full 4-second loop has completed.
     */
    private fun handleLoopFinished() {
        if (isDismissed) return
        Log.d(TAG, "handleLoopFinished() triggered - 4s loop completed")
        handler.removeCallbacksAndMessages(null)
        onLoopListener?.invoke(1)

        if (autoDismissAfterOneLoop) {
            dismiss(animate = true)
        }
    }

    /**
     * Start playing the splash screen animation.
     *
     * @param onDismiss Optional callback invoked when the splash screen is auto-dismissed.
     */
    fun play(onDismiss: (() -> Unit)? = null) {
        if (onDismiss != null) {
            this.onDismissListener = onDismiss
        }
        isDismissed = false
        visibility = View.VISIBLE
        alpha = 1f
        playStartTime = SystemClock.uptimeMillis()

        // Start Lottie playback
        lottieView.playAnimation()

        // Enforce the 4-second loop timer so auto-dismiss fires at exactly 4000ms
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (!isDismissed) {
                Log.d(TAG, "4-second timer completed")
                handleLoopFinished()
            }
        }, ANIMATION_LOOP_DURATION_MS)

        Log.d(TAG, "SplashScreen play() started at $playStartTime, timer scheduled for ${ANIMATION_LOOP_DURATION_MS}ms")
    }

    /**
     * Pause the animation.
     */
    fun pause() {
        lottieView.pauseAnimation()
    }

    /**
     * Resume the animation.
     */
    fun resume() {
        if (!isDismissed) {
            lottieView.resumeAnimation()
        }
    }

    /**
     * Set a callback to be triggered when the splash screen dismisses.
     */
    fun setOnDismissListener(listener: () -> Unit) {
        this.onDismissListener = listener
    }

    /**
     * Set a listener to observe loop completion.
     */
    fun setOnLoopListener(listener: (loopCount: Int) -> Unit) {
        this.onLoopListener = listener
    }

    /**
     * Dismiss the splash screen with a smooth fade-out.
     *
     * @param animate Whether to animate the dismissal with a fade transition.
     * @param onDismissed Optional immediate callback after dismissal finishes.
     */
    fun dismiss(animate: Boolean = true, onDismissed: (() -> Unit)? = null) {
        if (isDismissed) return
        isDismissed = true
        handler.removeCallbacksAndMessages(null)

        Log.d(TAG, "SplashScreen dismiss() called, animate=$animate")

        if (animate && dismissFadeDuration > 0) {
            animate()
                .alpha(0f)
                .setDuration(dismissFadeDuration)
                .withEndAction {
                    visibility = View.GONE
                    lottieView.pauseAnimation()
                    onDismissed?.invoke()
                    onDismissListener?.invoke()
                }
                .start()
        } else {
            visibility = View.GONE
            lottieView.pauseAnimation()
            onDismissed?.invoke()
            onDismissListener?.invoke()
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }
}

/** Aliases for convenient usage */
typealias SplashScreenComponent = SplashScreen
typealias SplashScreenView = SplashScreen
