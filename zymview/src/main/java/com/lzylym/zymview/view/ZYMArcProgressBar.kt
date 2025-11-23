package com.lzylym.zymview.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.lzylym.zymview.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class ZYMArcProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var max = 100f
    private var min = 0f

    private var progress = 0f
    private var targetProgress = 0f

    private var strokeWidth = 20f

    private var animator: ValueAnimator? = null
    private var animDuration = 1000L

    private var progressColorInt = Color.GREEN
    private var progressDrawable: Drawable? = null

    private var trackColor = Color.LTGRAY
    private var startAngle = -90f
    private var maxSweepAngle = 360f
    private var isRoundCap = false

    private var showLabels = false
    private var labelCount = 6
    private var labelTextSize = 12f.sp
    private var labelDistance = 4f.dp
    private var labelActiveColor = Color.BLACK
    private var labelInactiveColor = Color.GRAY

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ZYMArcProgressBar)

        max = ta.getFloat(R.styleable.ZYMArcProgressBar_zym_max, 100f)
        min = ta.getFloat(R.styleable.ZYMArcProgressBar_zym_min, 0f)
        targetProgress = ta.getFloat(R.styleable.ZYMArcProgressBar_zym_progress, min).coerceIn(min, max)
        progress = targetProgress

        strokeWidth = ta.getDimension(R.styleable.ZYMCommon_zym_stroke_width, 20f)

        if (ta.hasValue(R.styleable.ZYMArcProgressBar_zym_progress_color)) {
            val value = ta.peekValue(R.styleable.ZYMArcProgressBar_zym_progress_color)
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                progressColorInt = ta.getColor(R.styleable.ZYMArcProgressBar_zym_progress_color, Color.GREEN)
            } else {
                val resId = ta.getResourceId(R.styleable.ZYMArcProgressBar_zym_progress_color, 0)
                if (resId != 0) {
                    progressDrawable = ContextCompat.getDrawable(context, resId)
                }
            }
        }

        trackColor = ta.getColor(R.styleable.ZYMArcProgressBar_zym_track_color, Color.parseColor("#E0E0E0"))
        startAngle = ta.getFloat(R.styleable.ZYMArcProgressBar_zym_start_angle, -90f)
        maxSweepAngle = ta.getFloat(R.styleable.ZYMArcProgressBar_zym_sweep_angle, 360f)
        isRoundCap = ta.getBoolean(R.styleable.ZYMArcProgressBar_zym_round_cap, false)

        showLabels = ta.getBoolean(R.styleable.ZYMArcProgressBar_zym_show_labels, false)
        labelCount = ta.getInt(R.styleable.ZYMArcProgressBar_zym_label_count, 6).coerceAtLeast(2)
        labelTextSize = ta.getDimension(R.styleable.ZYMArcProgressBar_zym_label_text_size, 12f.sp)
        labelDistance = ta.getDimension(R.styleable.ZYMArcProgressBar_zym_label_distance, 4f.dp)
        labelActiveColor = ta.getColor(R.styleable.ZYMArcProgressBar_zym_label_active_color, Color.BLACK)
        labelInactiveColor = ta.getColor(R.styleable.ZYMArcProgressBar_zym_label_inactive_color, Color.GRAY)

        ta.recycle()
        initPaints()
    }

    private fun initPaints() {
        updatePaintStyles()

        if (progressDrawable != null) {
            updateProgressShader()
        } else {
            progressPaint.color = progressColorInt
        }

        labelPaint.textSize = labelTextSize
        labelPaint.textAlign = Paint.Align.CENTER
    }

    private fun updatePaintStyles() {
        trackPaint.style = Paint.Style.STROKE
        trackPaint.strokeWidth = strokeWidth
        trackPaint.color = trackColor
        trackPaint.strokeCap = if (isRoundCap && abs(maxSweepAngle) < 360) Paint.Cap.ROUND else Paint.Cap.BUTT

        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = strokeWidth
        progressPaint.strokeCap = if (isRoundCap) Paint.Cap.ROUND else Paint.Cap.BUTT
    }

    private fun updateProgressShader() {
        progressDrawable?.let { drawable ->
            if (width == 0 || height == 0) return
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val size = width.coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                bmp
            }
            val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            progressPaint.shader = shader
            progressPaint.color = Color.WHITE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateRectF(w, h)
        if (progressDrawable != null) {
            updateProgressShader()
        }
    }

    private fun calculateRectF(w: Int, h: Int) {
        val padding = strokeWidth / 2
        rectF.set(
            padding + paddingLeft,
            padding + paddingTop,
            w - padding - paddingRight,
            h - padding - paddingBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(rectF, startAngle, maxSweepAngle, false, trackPaint)

        val range = max - min
        val currentVal = (progress - min).coerceIn(0f, range)
        val ratio = if (range > 0) currentVal / range else 0f
        val currentSweepAngle = maxSweepAngle * ratio

        if (abs(currentSweepAngle) > 0.1f) {
            canvas.drawArc(rectF, startAngle, currentSweepAngle, false, progressPaint)
        }

        if (showLabels) {
            drawLabels(canvas)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        val centerX = rectF.centerX()
        val centerY = rectF.centerY()
        val radius = (rectF.width() / 2) - (strokeWidth / 2) - labelDistance - (labelTextSize / 2)

        val range = max - min
        val stepValue = range / (labelCount - 1)

        val isFullCircle = abs(maxSweepAngle) >= 360f
        val drawCount = if (isFullCircle) labelCount - 1 else labelCount

        val fontMetrics = labelPaint.fontMetrics
        val textDy = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent

        for (i in 0 until drawCount) {
            val tickValue = min + (i * stepValue)
            labelPaint.color = if (tickValue <= progress + 0.01f) labelActiveColor else labelInactiveColor

            val angleRatio = i.toFloat() / (labelCount - 1)
            val angleDeg = startAngle + (maxSweepAngle * angleRatio)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val x = centerX + radius * cos(angleRad).toFloat()
            val y = centerY + radius * sin(angleRad).toFloat()

            val text = tickValue.toInt().toString()
            canvas.drawText(text, x, y + textDy, labelPaint)
        }
    }

    private fun smoothUpdate() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, targetProgress).apply {
            duration = animDuration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                this@ZYMArcProgressBar.progress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun checkTargetProgress() {
        val clamped = targetProgress.coerceIn(min, max)
        if (clamped != targetProgress) {
            targetProgress = clamped
            smoothUpdate()
        } else {
            smoothUpdate()
        }
    }

    var minValue: Float
        get() = min
        set(value) {
            min = value
            checkTargetProgress()
        }

    var maxValue: Float
        get() = max
        set(value) {
            max = value
            checkTargetProgress()
        }

    fun setProgress(value: Float) {
        val newTarget = value.coerceIn(min, max)
        if (this.targetProgress != newTarget) {
            this.targetProgress = newTarget
            smoothUpdate()
        }
    }

    fun setProgressInstant(value: Float) {
        animator?.cancel()
        val newTarget = value.coerceIn(min, max)
        this.targetProgress = newTarget
        this.progress = newTarget
        invalidate()
    }

    fun setStrokeWidth(widthDp: Float) {
        this.strokeWidth = widthDp.dp
        calculateRectF(width, height)
        updatePaintStyles()
        invalidate()
    }

    fun setProgressColor(color: Int) {
        this.progressColorInt = color
        this.progressDrawable = null
        progressPaint.shader = null
        progressPaint.color = color
        invalidate()
    }

    fun setProgressDrawable(drawable: Drawable) {
        this.progressDrawable = drawable
        updateProgressShader()
        invalidate()
    }

    fun setTrackColor(color: Int) {
        this.trackColor = color
        trackPaint.color = color
        invalidate()
    }

    fun setAngles(startAngle: Float, sweepAngle: Float) {
        this.startAngle = startAngle
        this.maxSweepAngle = sweepAngle
        updatePaintStyles()
        invalidate()
    }

    fun setRoundCap(enable: Boolean) {
        this.isRoundCap = enable
        updatePaintStyles()
        invalidate()
    }

    fun setAnimDuration(duration: Long) {
        this.animDuration = duration
    }

    fun setShowLabels(show: Boolean) {
        this.showLabels = show
        invalidate()
    }

    fun setLabelConfig(min: Float, max: Float, count: Int) {
        this.min = min
        this.max = max
        this.labelCount = count.coerceAtLeast(2)
        checkTargetProgress()
    }

    fun setLabelTextSize(sizeSp: Float) {
        this.labelTextSize = sizeSp.sp
        labelPaint.textSize = this.labelTextSize
        invalidate()
    }

    fun setLabelDistance(distanceDp: Float) {
        this.labelDistance = distanceDp.dp
        invalidate()
    }

    fun setLabelColors(activeColor: Int, inactiveColor: Int) {
        this.labelActiveColor = activeColor
        this.labelInactiveColor = inactiveColor
        invalidate()
    }

    private val Float.dp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics
        )

    private val Float.sp: Float
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this, resources.displayMetrics
        )
}