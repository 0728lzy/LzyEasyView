package com.lzylym.zymview.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.lzylym.zymview.R
import kotlin.math.*

class ZYMClockSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class ClockMode { HOUR, MINUTE, HOUR_MINUTE }

    private var clockMode = ClockMode.HOUR
    private var selectedIndex = 0
    private var currentAngle = -90f
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCircle = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLine = Paint(Paint(Paint.ANTI_ALIAS_FLAG))
    private val paintDot = Paint(Paint(Paint.ANTI_ALIAS_FLAG))
    private val paintParticle = Paint(Paint(Paint.ANTI_ALIAS_FLAG))

    private val hourLabels = (0..11).map { String.format("%02d", it) }
    private val minuteLabels = listOf(
        "00", "05", "10", "15", "20", "25",
        "30", "35", "40", "45", "50", "55"
    )

    private var zymTextSize = 42f
    private var zymTextColor = Color.DKGRAY
    private var zymCircleRadius = 40f
    private var zymCircleColor = Color.parseColor("#2A55F3")
    private var zymPointerWidth = 6f
    private var zymPointerColor = Color.parseColor("#4C36F2")
    private var zymCenterDotColor = Color.BLACK

    private var pointerInnerPadding = 0f
    private var pointerOuterPadding = 0f

    var dragStartRatio = 0.5f
        set(value) { field = value.coerceIn(0f, 1f) }

    private var particles = mutableListOf<Particle>()
    private var isExploding = false
    private var isDragging = false
    private val handler = Handler(Looper.getMainLooper())
    private val explosionDelay = 1000L
    private val explosionDuration = 1000L

    private var lastHourIndex = 0

    private var lastReportedValue: String = ""
    private var lastReportedHour: Int = -1
    private var lastReportedMinute: Int = -1

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ZYMClockSelectorView)
            zymTextSize = ta.getDimension(R.styleable.ZYMClockSelectorView_zym_textSize, zymTextSize)
            zymTextColor = ta.getColor(R.styleable.ZYMClockSelectorView_zym_textColor, zymTextColor)
            zymCircleRadius = ta.getDimension(R.styleable.ZYMClockSelectorView_zym_circleRadius, zymCircleRadius)
            zymCircleColor = ta.getColor(R.styleable.ZYMClockSelectorView_zym_circleColor, zymCircleColor)
            zymPointerWidth = ta.getDimension(R.styleable.ZYMClockSelectorView_zym_pointerWidth, zymPointerWidth)
            zymPointerColor = ta.getColor(R.styleable.ZYMClockSelectorView_zym_pointerColor, zymPointerColor)
            zymCenterDotColor = ta.getColor(R.styleable.ZYMClockSelectorView_zym_centerDotColor, zymCenterDotColor)
            pointerInnerPadding = ta.getFloat(R.styleable.ZYMClockSelectorView_zym_pointerInnerPadding, 0f).coerceIn(0f, 0.5f)
            pointerOuterPadding = ta.getFloat(R.styleable.ZYMClockSelectorView_zym_pointerOuterPadding, 0f).coerceIn(0f, 0.5f)
            dragStartRatio = ta.getFloat(R.styleable.ZYMClockSelectorView_zym_dragStartRatio, 0.5f).coerceIn(0f, 1f)
            val mode = ta.getInt(R.styleable.ZYMClockSelectorView_zym_mode, 0)
            clockMode = when(mode) {
                0 -> ClockMode.HOUR
                1 -> ClockMode.MINUTE
                2 -> ClockMode.HOUR_MINUTE
                else -> ClockMode.HOUR
            }
            ta.recycle()
        }

        paintText.textSize = zymTextSize
        paintText.textAlign = Paint.Align.CENTER
        paintLine.strokeWidth = zymPointerWidth
        paintLine.color = zymPointerColor
        paintDot.color = zymCenterDotColor
        paintCircle.color = zymCircleColor
        paintParticle.color = zymCircleColor

        selectedIndex = 0
        lastHourIndex = 0
        if (clockMode == ClockMode.HOUR || clockMode == ClockMode.MINUTE) {
            lastReportedValue = getSelectedValue()
            lastReportedHour = -1
        } else if (clockMode == ClockMode.HOUR_MINUTE) {
            lastReportedHour = 0
            lastReportedMinute = getSelectedMinuteIndex()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) * 0.4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val labels = when(clockMode) {
            ClockMode.HOUR, ClockMode.HOUR_MINUTE -> hourLabels
            ClockMode.MINUTE -> minuteLabels
        }
        val stepAngle = 360f / labels.size
        val fontMetrics = paintText.fontMetrics
        val textCenterOffset = (fontMetrics.descent + fontMetrics.ascent) / 2

        val circleX = (centerX + radius * cos(Math.toRadians(currentAngle.toDouble()))).toFloat()
        val circleY = (centerY + radius * sin(Math.toRadians(currentAngle.toDouble()))).toFloat()

        if (!isExploding) {
            canvas.drawCircle(circleX, circleY, zymCircleRadius, paintCircle)
        } else {
            particles.forEach { it.draw(canvas, paintParticle) }
        }

        labels.forEachIndexed { index, text ->
            val angle = Math.toRadians((index * stepAngle - 90).toDouble())
            val x = (centerX + radius * cos(angle)).toFloat()
            val y = (centerY + radius * sin(angle) - textCenterOffset).toFloat()
            val dist = hypot(x - circleX, y - circleY)
            paintText.color = if (dist < zymCircleRadius * 0.9f) Color.WHITE else zymTextColor
            canvas.drawText(text, x, y, paintText)
        }

        val dx = circleX - centerX
        val dy = circleY - centerY
        val totalLen = hypot(dx, dy)
        val nearLen = totalLen - zymCircleRadius
        val startLen = nearLen * pointerInnerPadding
        val endLen = nearLen * (1f - pointerOuterPadding)
        val startX = centerX + dx / totalLen * startLen
        val startY = centerY + dy / totalLen * startLen
        val endX = centerX + dx / totalLen * endLen
        val endY = centerY + dy / totalLen * endLen
        canvas.drawLine(startX, startY, endX, endY, paintLine)

        canvas.drawCircle(centerX, centerY, 10f, paintDot)

        if (isExploding) {
            particles.forEach { it.update() }
            if (particles.all { it.alpha <= 0 }) {
                isExploding = false
            }
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val dx = event.x - centerX
        val dy = event.y - centerY
        val touchRadius = hypot(dx, dy)
        val circleX = (centerX + radius * cos(Math.toRadians(currentAngle.toDouble()))).toFloat()
        val circleY = (centerY + radius * sin(Math.toRadians(currentAngle.toDouble()))).toFloat()
        val touchOnCircle = hypot(dx - (circleX - centerX), dy - (circleY - centerY)) <= zymCircleRadius * 1.5
        val centerRadius = 30f
        val touchOnCenter = hypot(dx, dy) <= centerRadius

        val minDragRadius = radius * 0.3f
        val maxDragRadius = radius * 1.1f

        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (touchOnCenter && clockMode == ClockMode.MINUTE) {
                    triggerCenterClickExplosion()
                    return true
                }

                if (touchRadius !in minDragRadius..maxDragRadius && !touchOnCircle) return false
                parent.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchRadius in minDragRadius..maxDragRadius || touchOnCircle) {
                    val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360) % 360
                    currentAngle = angle

                    if (isExploding) return true

                    when (clockMode) {
                        ClockMode.HOUR -> {
                            updateHourIndex()
                            updateSingleValueCallback()
                        }
                        ClockMode.MINUTE -> {
                            updateMinuteIndex()
                            if (lastReportedHour != -1 && lastReportedHour != -99) {
                                updateHourMinuteCallback(isShowingHour = false)
                            } else {
                                updateSingleValueCallback()
                            }
                        }
                        ClockMode.HOUR_MINUTE -> {
                            updateHourIndex()
                            updateHourMinuteCallback(isShowingHour = true)
                        }
                    }

                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                val angle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360) % 360
                currentAngle = angle

                if (clockMode == ClockMode.HOUR || clockMode == ClockMode.HOUR_MINUTE) {
                    snapToNearest()

                    if (clockMode == ClockMode.HOUR_MINUTE) {
                        handler.postDelayed({
                            triggerExplosionAndSwitchMode()
                        }, explosionDelay)
                    }
                } else {
                    updateMinuteIndex()

                    if (lastReportedHour != -1 && lastReportedHour != -99) {
                        updateHourMinuteCallback(isShowingHour = false)
                    } else {
                        updateSingleValueCallback()
                    }
                }
            }
        }
        return true
    }

    private fun updateHourIndex() {
        val labels = hourLabels
        val step = 360f / labels.size
        var minDiff = 360f
        var nearestIndex = 0
        for (i in labels.indices) {
            val targetAngle = i * step - 90
            val diff = abs(((currentAngle - targetAngle + 540) % 360) - 180)
            if (diff < minDiff) {
                minDiff = diff
                nearestIndex = i
            }
        }
        selectedIndex = nearestIndex
        lastHourIndex = selectedIndex
    }

    private fun snapToNearest() {
        val labels = when(clockMode) {
            ClockMode.HOUR, ClockMode.HOUR_MINUTE -> hourLabels
            ClockMode.MINUTE -> minuteLabels
        }
        val step = 360f / labels.size
        var minDiff = 360f
        var nearestIndex = 0
        for (i in labels.indices) {
            val targetAngle = i * step - 90
            val diff = abs(((currentAngle - targetAngle + 540) % 360) - 180)
            if (diff < minDiff) {
                minDiff = diff
                nearestIndex = i
            }
        }
        selectedIndex = nearestIndex

        if (clockMode == ClockMode.HOUR || clockMode == ClockMode.HOUR_MINUTE) {
            lastHourIndex = selectedIndex
        }

        val targetAngle = nearestIndex * step - 90
        animateTo(targetAngle, 200L) {
            if (clockMode == ClockMode.HOUR) {
                updateSingleValueCallback()
            } else if (clockMode == ClockMode.HOUR_MINUTE) {
                updateHourMinuteCallback(isShowingHour = true)
            }
        }
    }


    private fun updateMinuteIndex() {
        val angleFromTop = (currentAngle + 90 + 360) % 360
        val stepAngle = 360f / minuteLabels.size
        val index = floor(angleFromTop / stepAngle).toInt() % 12
        selectedIndex = index
    }

    private fun animateTo(targetAngle: Float, duration: Long, onEnd: () -> Unit = {}) {
        val start = currentAngle
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = duration
        anim.addUpdateListener {
            val t = it.animatedValue as Float
            currentAngle = smoothAngle(start, targetAngle, t)
            invalidate()
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onEnd()
            }
        })
        anim.start()
    }

    private fun smoothAngle(a: Float, b: Float, t: Float): Float {
        val a360 = (a + 360) % 360
        val b360 = (b + 360) % 360
        var diff = b360 - a360
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360
        return (a360 + diff * t + 360) % 360
    }

    fun getSelectedValue(): String {
        return when(clockMode) {
            ClockMode.HOUR -> hourLabels[selectedIndex]
            ClockMode.HOUR_MINUTE -> hourLabels[selectedIndex]
            ClockMode.MINUTE -> {
                val angleFromTop = (currentAngle + 90 + 360) % 360
                val stepAngle = 360f / minuteLabels.size
                val index = floor(angleFromTop / stepAngle).toInt() % 12
                val startMinute = index * 5
                val intraAngle = angleFromTop - index * stepAngle
                val minute = startMinute + ((intraAngle / stepAngle) * 5).roundToInt()
                (minute % 60).toString().padStart(2, '0')
            }
        }
    }

    private fun updateSingleValueCallback() {
        val currentValue = getSelectedValue()
        if (currentValue != lastReportedValue) {
            listener?.onZYMValueSelected(currentValue)
            lastReportedValue = currentValue
        }
    }

    private fun updateHourMinuteCallback(isShowingHour: Boolean) {
        val currentHour = lastHourIndex

        val currentMinute = if (isShowingHour) {
            0
        } else {
            getSelectedMinuteIndex()
        }

        if (currentHour != lastReportedHour || currentMinute != lastReportedMinute) {
            listener?.onHourMinuteSelected(currentHour, currentMinute, isShowingHour)
            lastReportedHour = currentHour
            lastReportedMinute = currentMinute
        }
    }

    private fun getSelectedMinuteIndex(): Int {
        val angleFromTop = (currentAngle + 90 + 360) % 360
        val stepAngle = 360f / minuteLabels.size
        val index = floor(angleFromTop / stepAngle).toInt() % 12
        val startMinute = index * 5
        val intraAngle = angleFromTop - index * stepAngle
        val minute = startMinute + ((intraAngle / stepAngle) * 5).roundToInt()
        return minute % 60
    }

    interface OnZYMValueSelectedListener {
        fun onZYMValueSelected(value: String)
        fun onHourMinuteSelected(hour: Int, minute: Int, isShowingHour: Boolean)
    }

    private var listener: OnZYMValueSelectedListener? = null

    fun setOnZYMValueSelectedListener(l: OnZYMValueSelectedListener) {
        listener = l

        if (clockMode == ClockMode.HOUR || clockMode == ClockMode.MINUTE) {
            l.onZYMValueSelected(getSelectedValue())
        } else if (clockMode == ClockMode.HOUR_MINUTE) {
            l.onHourMinuteSelected(lastHourIndex, getSelectedMinuteIndex(), clockMode == ClockMode.HOUR_MINUTE)
        }
    }

    fun setPointerPadding(inner: Float, outer: Float) {
        pointerInnerPadding = inner.coerceIn(0f, 0.5f)
        pointerOuterPadding = outer.coerceIn(0f, 0.5f)
        invalidate()
    }

    private fun triggerExplosionAndSwitchMode() {
        isExploding = true
        particles.clear()
        val circleX = (centerX + radius * cos(Math.toRadians(currentAngle.toDouble()))).toFloat()
        val circleY = (centerY + radius * sin(Math.toRadians(currentAngle.toDouble()))).toFloat()
        repeat(30) { particles.add(Particle(circleX, circleY)) }

        clockMode = ClockMode.MINUTE
        updateMinuteIndex()
        updateHourMinuteCallback(isShowingHour = false)

        handler.postDelayed({
            isExploding = false
            invalidate()
        }, explosionDuration)
        invalidate()
    }

    private fun triggerCenterClickExplosion() {
        isExploding = true
        particles.clear()
        val circleX = (centerX + radius * cos(Math.toRadians(currentAngle.toDouble()))).toFloat()
        val circleY = (centerY + radius * sin(Math.toRadians(currentAngle.toDouble()))).toFloat()
        repeat(30) { particles.add(Particle(circleX, circleY)) }

        invalidate()

        handler.postDelayed({
            isExploding = false
            if (clockMode == ClockMode.MINUTE) {
                clockMode = ClockMode.HOUR_MINUTE
                selectedIndex = lastHourIndex
                val targetAngle = selectedIndex * 30f - 90

                animateTo(targetAngle, 200L) {
                    updateHourMinuteCallback(isShowingHour = true)
                }
            }
        }, explosionDuration)
    }

    private class Particle(var x: Float, var y: Float) {
        var vx = (-5..5).random().toFloat()
        var vy = (-5..5).random().toFloat()
        var alpha = 255
        var radius = (4..8).random().toFloat()

        fun update() {
            x += vx
            y += vy
            alpha -= 15
            if (alpha < 0) alpha = 0
        }

        fun draw(canvas: Canvas, paint: Paint) {
            paint.alpha = alpha
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}