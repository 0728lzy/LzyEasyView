package com.lzylym.zymview.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class LZYCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** N 标签颜色（默认 FE6B47） */
    var nLabelColor: Int = Color.parseColor("#FE6B47")
        set(value) {
            field = value
            invalidate()
        }

    /** S 标签颜色（默认 6199D2） */
    var sLabelColor: Int = Color.parseColor("#6199D2")
        set(value) {
            field = value
            invalidate()
        }

    /** 当前旋转角度（0 = 北） */
    private var degree: Float = 0f

    fun setDegree(angle: Float) {
        degree = angle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (Math.min(width, height) / 2f) - 5f

        // --- 横向渐变圆盘 ---
        val shader = LinearGradient(
            cx - radius, cy, cx + radius, cy,
            intArrayOf(
                Color.parseColor("#428BDA"),
                Color.parseColor("#4085D1"),
                Color.parseColor("#3981CB")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        circlePaint.shader = shader
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // --- 白色边框 ---
        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = Color.WHITE
        borderPaint.strokeWidth = 6f
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // 旋转画布
        canvas.save()
        canvas.rotate(-degree, cx, cy)

        val labelWidth = radius * 0.24f
        val labelHeight = radius * 0.18f
        val cornerRadius = 20f
        val edgeMargin = dp2px(8f)  // 🔹 距离圆盘边缘 4dp

        // --- N 标签（下圆角） ---
        val nRect = RectF(
            cx - labelWidth / 2,
            cy - radius + edgeMargin,
            cx + labelWidth / 2,
            cy - radius + edgeMargin + labelHeight
        )
        labelPaint.color = nLabelColor
        val nPath = Path().apply {
            addRoundRect(
                nRect,
                floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }
        canvas.drawPath(nPath, labelPaint)
        canvas.drawText("N", nRect.centerX(), nRect.centerY() + textPaint.textSize / 3, textPaint)

        // --- S 标签（上圆角） ---
        val sRect = RectF(
            cx - labelWidth / 2,
            cy + radius - edgeMargin - labelHeight,
            cx + labelWidth / 2,
            cy + radius - edgeMargin
        )
        labelPaint.color = sLabelColor
        val sPath = Path().apply {
            addRoundRect(
                sRect,
                floatArrayOf(0f, 0f, 0f, 0f, cornerRadius, cornerRadius, cornerRadius, cornerRadius),
                Path.Direction.CW
            )
        }
        canvas.drawPath(sPath, labelPaint)
        canvas.drawText("S", sRect.centerX(), sRect.centerY() + textPaint.textSize / 3, textPaint)

        canvas.restore()
    }

    private fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
