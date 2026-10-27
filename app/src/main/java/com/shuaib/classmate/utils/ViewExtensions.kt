package com.shuaib.classmate.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.shuaib.classmate.R

/**
 * C:/Users/USER/AndroidStudioProjects/ClassMate/app/src/main/java/com/shuaib/classmate/utils/ViewExtensions.kt
 */
fun View.animateSpringScale(targetScale: Float) {
    val springX = SpringAnimation(this, SpringAnimation.SCALE_X, 1f).apply {
        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        spring.stiffness = SpringForce.STIFFNESS_MEDIUM
    }
    val springY = SpringAnimation(this, SpringAnimation.SCALE_Y, 1f).apply {
        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        spring.stiffness = SpringForce.STIFFNESS_MEDIUM
    }

    this.scaleX = targetScale
    this.scaleY = targetScale
    springX.start()
    springY.start()
}

/**
 * Tactile macOS-style spring scale bounce animation before launching composers and writing options.
 */
fun View.applyComposerBounceAnimation(onComplete: () -> Unit) {
    this.animate()
        .scaleX(0.82f).scaleY(0.82f)
        .setDuration(120)
        .withEndAction {
            this.animate()
                .scaleX(1.0f).scaleY(1.0f)
                .setDuration(180)
                .start()
            onComplete()
        }
        .start()
}

/**
 * Apply the smooth floating expanding window animation matching CampusFeed post composer.
 */
fun Dialog.applyFloatingPostAnimation(
    context: Context,
    widthPercent: Double = 0.94,
    heightPercent: Double = 0.78,
    isWrapHeight: Boolean = false
) {
    window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val width = (context.resources.displayMetrics.widthPixels * widthPercent).toInt()
        val height = if (isWrapHeight) {
            ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            (context.resources.displayMetrics.heightPixels * heightPercent).toInt()
        }
        setLayout(width, height)
        setGravity(Gravity.CENTER)
        attributes?.windowAnimations = R.style.FloatingPostDialogAnimation
    }
}

fun View.applyClickAnimation(onClick: () -> Unit) {
    val reduceMotion = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ) == 0f

    setOnClickListener {
        onClick()
        if (!reduceMotion) {
            SpringAnimation(this, DynamicAnimation.SCALE_X, 1f).apply {
                spring.stiffness = SpringForce.STIFFNESS_HIGH
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                this@applyClickAnimation.scaleX = 0.93f
                start()
            }
            SpringAnimation(this, DynamicAnimation.SCALE_Y, 1f).apply {
                spring.stiffness = SpringForce.STIFFNESS_HIGH
                spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                this@applyClickAnimation.scaleY = 0.93f
                start()
            }
        }
    }
}

/**
 * iOS-style tactile spring scale bounce animation for dashboard cards and shortcut tiles.
 */
fun View.applyCardClickAnimation(onClick: () -> Unit) {
    HapticHelper.lightPop(this)
    this.animate().cancel()
    this.animate()
        .scaleX(0.91f)
        .scaleY(0.91f)
        .setDuration(110)
        .setInterpolator(DecelerateInterpolator())
        .withEndAction {
            this.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(160)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()
            onClick()
        }
        .start()
}

/**
 * iOS-style tactile spring card click animation that launches an Activity with scale-up transition.
 */
fun View.launchActivityWithCardAnimation(intent: Intent) {
    HapticHelper.lightPop(this)
    this.animate().cancel()
    this.animate()
        .scaleX(0.91f)
        .scaleY(0.91f)
        .setDuration(110)
        .setInterpolator(DecelerateInterpolator())
        .withEndAction {
            this.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(160)
                .setInterpolator(OvershootInterpolator(1.4f))
                .start()

            val act = context as? Activity
            val options = ActivityOptionsCompat.makeScaleUpAnimation(
                this,
                0,
                0,
                width.coerceAtLeast(1),
                height.coerceAtLeast(1)
            )
            context.startActivity(intent, options.toBundle())
            act?.overridePendingTransition(R.anim.anim_ios_card_open_enter, R.anim.anim_ios_card_open_exit)
        }
        .start()
}
