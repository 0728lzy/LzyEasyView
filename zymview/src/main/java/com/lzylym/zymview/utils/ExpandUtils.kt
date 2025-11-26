package com.lzylym.zymview.utils

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.view.View
import android.view.animation.LinearInterpolator

object ExpandUtils {
    fun View.rotateAround(
        leftDp: Float,
        bottomDp: Float,
        startAngle: Float,
        endAngle: Float,
        duration: Long
    ) {
        val leftPx = leftDp * Resources.getSystem().displayMetrics.density
        val bottomPx = bottomDp * Resources.getSystem().displayMetrics.density
        this.post {
            this.pivotX = leftPx
            this.pivotY = this.height - bottomPx
            val animator = ObjectAnimator.ofFloat(this, View.ROTATION, startAngle, endAngle)
            animator.interpolator = LinearInterpolator() // 设置匀速
            animator.duration = duration
            animator.start()
        }
    }
}