package com.lzylym.zymview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.lzylym.zymview.R
import kotlin.math.abs
import kotlin.math.min

class ZYMCompassView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetAngle: Float = 0f
    private var dialAngle: Float = 0f
    private var needleAngle: Float = 0f
    private var needleVelocity: Float = 0f

    private val NEEDLE_TENSION = 0.04f
    private val NEEDLE_DAMPING = 0.90f
    private val DIAL_SMOOTHING = 0.2f

    var dialBackgroundColor: Int = Color.WHITE
        set(value) { field = value; invalidate() }

    var cardinalTextSize: Float = sp2px(16f)
        set(value) { field = value; invalidate() }

    var numberTextSize: Float = sp2px(14f)
        set(value) { field = value; invalidate() }

    var northColor: Int = Color.parseColor("#D81B60")
        set(value) {
            field = value
            paintNorth.color = value
            paintNeedleArrow.color = value
            invalidate()
        }

    var pointerScale: Float = 0.7f
        set(value) { field = value.coerceIn(0.1f, 1.0f); invalidate() }

    var outerCircleGap: Float = dp2px(3f)
        set(value) { field = value; invalidate() }

    private var colorNormalText: Int = Color.parseColor("#333333")
    private var colorTickMajor: Int = Color.parseColor("#333333")
    private var colorTickMinor: Int = Color.parseColor("#999999")
    private var colorCircleBorder: Int = Color.parseColor("#CCCCCC")
    private var colorSouth: Int = Color.parseColor("#333333")

    private val paintBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintTick = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintIndicator = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintNorth = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintNeedleArrow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintSouth = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintCenterRing = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private val centerRingRadius = dp2px(6f)

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ZYMCompassView)
        dialBackgroundColor = typedArray.getColor(R.styleable.ZYMCompassView_zym_dialBackgroundColor, Color.WHITE)
        cardinalTextSize = typedArray.getDimension(R.styleable.ZYMCompassView_zym_cardinalTextSize, sp2px(16f))
        numberTextSize = typedArray.getDimension(R.styleable.ZYMCompassView_zym_numberTextSize, dp2px(14f))
        northColor = typedArray.getColor(R.styleable.ZYMCompassView_zym_northColor, Color.parseColor("#D81B60"))
        pointerScale = typedArray.getFloat(R.styleable.ZYMCompassView_zym_pointerScale, 0.7f)
        outerCircleGap = typedArray.getDimension(R.styleable.ZYMCompassView_zym_outerCircleGap, dp2px(3f))
        typedArray.recycle()

        paintBackground.style = Paint.Style.FILL
        paintBackground.color = dialBackgroundColor

        paintText.textAlign = Paint.Align.CENTER
        paintTick.style = Paint.Style.STROKE
        paintTick.strokeCap = Paint.Cap.ROUND

        paintBorder.style = Paint.Style.STROKE
        paintBorder.strokeWidth = dp2px(1f)
        paintBorder.color = colorCircleBorder

        paintIndicator.style = Paint.Style.FILL
        paintIndicator.color = northColor

        paintNorth.style = Paint.Style.STROKE
        paintNorth.strokeCap = Paint.Cap.BUTT
        paintNorth.strokeWidth = dp2px(3f)
        paintNorth.color = northColor

        paintNeedleArrow.style = Paint.Style.FILL
        paintNeedleArrow.color = northColor

        paintSouth.style = Paint.Style.STROKE
        paintSouth.strokeCap = Paint.Cap.BUTT
        paintSouth.strokeWidth = dp2px(3f)
        paintSouth.color = colorSouth

        paintCenterRing.style = Paint.Style.STROKE
        paintCenterRing.color = Color.parseColor("#333333")
        paintCenterRing.strokeWidth = dp2px(2f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(w, h)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = (min(w, h) / 2f) - dp2px(5f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val needInvalidate = calculatePhysics()

        paintBackground.color = dialBackgroundColor
        canvas.drawCircle(cx, cy, radius, paintBackground)
        canvas.drawCircle(cx, cy, radius, paintBorder)

        drawFixedTopIndicator(canvas)

        canvas.save()
        canvas.rotate(-dialAngle, cx, cy)
        drawDial(canvas)
        canvas.restore()

        canvas.save()
        canvas.rotate(-needleAngle, cx, cy)
        drawNeedle(canvas)
        canvas.restore()

        drawCenterRing(canvas)

        if (needInvalidate) {
            postInvalidateOnAnimation()
        }
    }

    private fun calculatePhysics(): Boolean {
        var isAnimating = false

        var dialDiff = targetAngle - dialAngle
        while (dialDiff < -180) dialDiff += 360
        while (dialDiff > 180) dialDiff -= 360

        if (abs(dialDiff) > 0.1f) {
            dialAngle += dialDiff * DIAL_SMOOTHING
            dialAngle = (dialAngle % 360 + 360) % 360
            isAnimating = true
        } else {
            dialAngle = targetAngle
        }

        var needleDiff = targetAngle - needleAngle
        while (needleDiff < -180) needleDiff += 360
        while (needleDiff > 180) needleDiff -= 360

        val acceleration = needleDiff * NEEDLE_TENSION
        needleVelocity += acceleration
        needleVelocity *= NEEDLE_DAMPING

        if (abs(needleDiff) > 0.1f || abs(needleVelocity) > 0.1f) {
            needleAngle += needleVelocity
            needleAngle = (needleAngle % 360 + 360) % 360
            isAnimating = true
        } else {
            needleAngle = targetAngle
            needleVelocity = 0f
        }

        return isAnimating
    }

    private fun drawDial(canvas: Canvas) {
        for (degree in 0 until 360 step 3) {
            canvas.save()
            canvas.rotate(degree.toFloat(), cx, cy)

            val isMajor = degree % 30 == 0
            val tickLen = if (isMajor) dp2px(12f) else dp2px(6f)

            paintTick.strokeWidth = if (isMajor) dp2px(2f) else dp2px(1f)
            paintTick.color = if (isMajor) colorTickMajor else colorTickMinor

            val startY = cy - radius + outerCircleGap
            val endY = startY + tickLen

            canvas.drawLine(cx, startY, cx, endY, paintTick)

            if (isMajor) {
                val textStartY = endY + outerCircleGap
                drawDegreeText(canvas, degree, textStartY)
            }
            canvas.restore()
        }
    }

    private fun drawNeedle(canvas: Canvas) {
        val lineLen = radius * pointerScale
        val arrowHeight = dp2px(12f)
        val arrowWidth = dp2px(7f)

        canvas.drawLine(cx, cy - centerRingRadius, cx, cy - lineLen, paintNorth)
        val pathNorth = Path()
        pathNorth.moveTo(cx, cy - lineLen - arrowHeight)
        pathNorth.lineTo(cx - arrowWidth, cy - lineLen)
        pathNorth.lineTo(cx + arrowWidth, cy - lineLen)
        pathNorth.close()
        canvas.drawPath(pathNorth, paintNeedleArrow)

        val southTotalLen = lineLen + arrowHeight
        canvas.drawLine(cx, cy + centerRingRadius, cx, cy + southTotalLen, paintSouth)
    }

    private fun drawDegreeText(canvas: Canvas, degree: Int, startY: Float) {
        val isCardinal = degree % 90 == 0
        if (isCardinal) {
            val text = when (degree) {
                0 -> "北"
                90 -> "东"
                180 -> "南"
                270 -> "西"
                else -> ""
            }
            paintText.textSize = cardinalTextSize
            paintText.typeface = Typeface.DEFAULT_BOLD
            paintText.color = if (degree == 0) northColor else colorNormalText
            val fontMetrics = paintText.fontMetrics
            val baseline = startY - fontMetrics.top
            canvas.drawText(text, cx, baseline, paintText)

            paintText.textSize = numberTextSize * 0.8f
            paintText.color = colorTickMinor
            paintText.typeface = Typeface.DEFAULT
            val smallNumHeight = paintText.fontMetrics.bottom - paintText.fontMetrics.top
            canvas.drawText(degree.toString(), cx, baseline + smallNumHeight * 0.8f, paintText)
        } else {
            paintText.textSize = numberTextSize
            paintText.typeface = Typeface.DEFAULT
            paintText.color = colorNormalText
            val fontMetrics = paintText.fontMetrics
            val baseline = startY - fontMetrics.top
            canvas.drawText(degree.toString(), cx, baseline, paintText)
        }
    }

    private fun drawFixedTopIndicator(canvas: Canvas) {
        val path = Path()
        val indicatorHeight = dp2px(10f)
        val tipY = cy - radius + dp2px(5f)
        path.moveTo(cx, tipY)
        path.lineTo(cx - dp2px(6f), tipY + indicatorHeight)
        path.lineTo(cx + dp2px(6f), tipY + indicatorHeight)
        path.close()
        canvas.drawPath(path, paintIndicator)
    }

    private fun drawCenterRing(canvas: Canvas) {
        canvas.drawCircle(cx, cy, centerRingRadius, paintCenterRing)
    }

    fun updateDirection(angle: Float) {
        this.targetAngle = angle
        postInvalidateOnAnimation()
    }

    fun setDirectionInstant(angle: Float) {
        this.targetAngle = angle
        this.dialAngle = angle
        this.needleAngle = angle
        this.needleVelocity = 0f
        invalidate()
    }

    private fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}