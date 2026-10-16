package com.shuaib.classmate.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Layout to wrap a scrollable component (e.g. ViewPager2) inside an outer ViewPager2.
 * Resolves touch/gesture interception conflicts so the nested ViewPager2 can be swiped smoothly
 * without the parent ViewPager2 stealing horizontal touch events.
 */
class NestedScrollableHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var touchSlop = 0
    private var initialX = 0f
    private var initialY = 0f

    private val parentViewPager: ViewPager2?
        get() {
            var v: View? = parent as? View
            while (v != null && v !is ViewPager2) {
                v = v.parent as? View
            }
            return v as? ViewPager2
        }

    private val child: View?
        get() = if (childCount > 0) getChildAt(0) else null

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private fun canChildScroll(orientation: Int, delta: Float): Boolean {
        val direction = -delta.sign.toInt()
        return when (orientation) {
            ORIENTATION_HORIZONTAL -> child?.canScrollHorizontally(direction) ?: false
            else -> child?.canScrollVertically(direction) ?: false
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        handleInterceptTouchEvent(e)
        return super.onInterceptTouchEvent(e)
    }

    private fun handleInterceptTouchEvent(e: MotionEvent) {
        val orientation = parentViewPager?.orientation ?: return

        if (e.action == MotionEvent.ACTION_DOWN) {
            initialX = e.x
            initialY = e.y
            parent.requestDisallowInterceptTouchEvent(true)
        } else if (e.action == MotionEvent.ACTION_MOVE) {
            val dx = e.x - initialX
            val dy = e.y - initialY
            val isVpHorizontal = orientation == ORIENTATION_HORIZONTAL

            val scaledDx = dx.absoluteValue * if (isVpHorizontal) .5f else 1f
            val scaledDy = dy.absoluteValue * if (isVpHorizontal) 1f else .5f

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is predominantly vertical: let the parent ScrollView scroll up/down
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    // Gesture is predominantly horizontal: keep gesture inside inner pager
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }
}
