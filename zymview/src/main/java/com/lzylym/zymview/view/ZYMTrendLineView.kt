package com.lzylym.zymview.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.lzylym.zymview.R
import java.text.DecimalFormat
import java.util.LinkedList

class ZYMTrendLineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class LineType { POLYLINE, SMOOTH }

    var axisTextMargin: Float = 8f.dp
        set(value) { field = value; requestLayout(); invalidate() }

    var maxValue: Float = 100f
        set(value) { field = value; requestLayout(); invalidate() }

    var minValue: Float = 0f
        set(value) { field = value; requestLayout(); invalidate() }

    var unit: String = ""
        set(value) { field = value; requestLayout(); invalidate() }

    var showYText: Boolean = true
        set(value) { field = value; requestLayout(); invalidate() }

    var axisTextSize: Float = 12f.sp
        set(value) { field = value; textPaint.textSize = value; requestLayout(); invalidate() }

    var yLabelCount: Int = 6
        set(value) { field = value.coerceAtLeast(2); requestLayout(); invalidate() }

    var axisTextColor: Int = Color.BLACK
        set(value) { field = value; textPaint.color = value; invalidate() }

    var strokeColor: Int = Color.parseColor("#AA00FF")
        set(value) { field = value; linePaint.color = value; invalidate() }

    var lineStrokeWidth: Float = 2f.dp
        set(value) { field = value; linePaint.strokeWidth = value; invalidate() }

    var fillStartColor: Int = Color.parseColor("#80AA00FF")
        set(value) { field = value; updateShader(); invalidate() }

    var fillEndColor: Int = Color.parseColor("#10AA00FF")
        set(value) { field = value; updateShader(); invalidate() }

    var visiblePointCount: Int = 10
        set(value) {
            field = value.coerceAtLeast(2)
            while (dataList.size > field) dataList.removeFirst()
            invalidate()
        }

    var lineType: LineType = LineType.POLYLINE
        set(value) { field = value; invalidate() }

    private val dataList = LinkedList<Float>()
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePath = Path()
    private val fillPath = Path()

    private var graphWidth = 0f
    private var graphHeight = 0f
    private var textAreaWidth = 0f
    private var verticalOffset = 0f

    private val decimalFormat = DecimalFormat("#.#")

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ZYMTrendLineView)

            axisTextMargin = ta.getDimension(R.styleable.ZYMTrendLineView_zym_axis_text_margin, 8f.dp)
            maxValue = ta.getFloat(R.styleable.ZYMTrendLineView_zym_max_value, 100f)
            minValue = ta.getFloat(R.styleable.ZYMTrendLineView_zym_min_value, 0f)
            unit = ta.getString(R.styleable.ZYMTrendLineView_zym_unit) ?: ""
            showYText = ta.getBoolean(R.styleable.ZYMTrendLineView_zym_show_y_text, true)
            axisTextSize = ta.getDimension(R.styleable.ZYMTrendLineView_zym_text_size, 12f.sp)
            yLabelCount = ta.getInt(R.styleable.ZYMTrendLineView_zym_y_label_count, 6)
            axisTextColor = ta.getColor(R.styleable.ZYMTrendLineView_zym_text_color, Color.BLACK)
            strokeColor = ta.getColor(R.styleable.ZYMTrendLineView_zym_stroke_color, Color.parseColor("#AA00FF"))
            lineStrokeWidth = ta.getDimension(R.styleable.ZYMCommon_zym_stroke_width, 2f.dp)
            fillStartColor = ta.getColor(R.styleable.ZYMTrendLineView_zym_fill_start_color, Color.parseColor("#80AA00FF"))
            fillEndColor = ta.getColor(R.styleable.ZYMTrendLineView_zym_fill_end_color, Color.parseColor("#10AA00FF"))
            visiblePointCount = ta.getInt(R.styleable.ZYMTrendLineView_zym_visible_point_count, 10)

            val typeIndex = ta.getInt(R.styleable.ZYMTrendLineView_zym_line_type, 0)
            lineType = if (typeIndex == 1) LineType.SMOOTH else LineType.POLYLINE

            ta.recycle()
        }

        initPaints()
    }

    private fun initPaints() {
        linePaint.color = strokeColor
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = lineStrokeWidth
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeJoin = Paint.Join.ROUND

        fillPaint.style = Paint.Style.FILL

        textPaint.color = axisTextColor
        textPaint.textSize = axisTextSize
        textPaint.textAlign = Paint.Align.RIGHT
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateDimensions(w, h)
        updateShader()
    }

    private fun calculateDimensions(w: Int, h: Int) {
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        verticalOffset = textHeight / 2

        textAreaWidth = if (showYText) {
            val maxText = "${decimalFormat.format(maxValue)}$unit"
            textPaint.measureText(maxText) + axisTextMargin
        } else 0f

        graphWidth = w - textAreaWidth - paddingRight - paddingLeft
        graphHeight = h.toFloat() - paddingTop - paddingBottom - textHeight
    }

    private fun updateShader() {
        if (graphHeight > 0) {
            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                fillStartColor, fillEndColor,
                Shader.TileMode.CLAMP
            )
            fillPaint.shader = gradient
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (showYText) drawYAxisLabels(canvas)
        if (dataList.isEmpty()) return

        preparePaths()

        canvas.save()
        canvas.translate(paddingLeft + textAreaWidth, paddingTop + verticalOffset)
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
        canvas.restore()
    }

    private fun drawYAxisLabels(canvas: Canvas) {
        val count = yLabelCount.coerceAtLeast(2)
        val range = maxValue - minValue
        val stepVal = range / (count - 1)
        val stepHeight = graphHeight / (count - 1)

        val fontMetrics = textPaint.fontMetrics
        val textCenterOffset = (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent

        for (i in 0 until count) {
            val rawValue = minValue + (i * stepVal)
            val text = "${decimalFormat.format(rawValue)}$unit"
            val y = graphHeight - (i * stepHeight) + paddingTop + verticalOffset
            val x = paddingLeft + textAreaWidth - axisTextMargin
            canvas.drawText(text, x, y + textCenterOffset, textPaint)
        }
    }

    private fun preparePaths() {
        linePath.reset()
        fillPath.reset()

        val xStep = if (visiblePointCount > 1) graphWidth / (visiblePointCount - 1) else graphWidth
        val valueRange = maxValue - minValue
        if (valueRange == 0f) return

        val points = mutableListOf<PointF>()
        for (i in dataList.indices) {
            val value = dataList[i]
            val ratio = (value - minValue) / valueRange
            val y = graphHeight - (ratio * graphHeight)
            val x = i * xStep
            points.add(PointF(x, y))
        }

        if (points.isEmpty()) return

        val firstX = points[0].x
        val lastX = points.last().x

        if (lineType == LineType.POLYLINE) {
            linePath.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, graphHeight)
            fillPath.lineTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                val p = points[i]
                linePath.lineTo(p.x, p.y)
                fillPath.lineTo(p.x, p.y)
            }
        } else {
            linePath.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, graphHeight)
            fillPath.lineTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val start = points[i]
                val end = points[i + 1]
                val controlX = (start.x + end.x) / 2
                linePath.cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
                fillPath.cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
            }
        }

        fillPath.lineTo(lastX, graphHeight)
        fillPath.lineTo(firstX, graphHeight)
        fillPath.close()
    }

    fun addData(value: Float) {
        if (value < minValue || value > maxValue) return
        dataList.add(value)
        if (dataList.size > visiblePointCount) dataList.removeFirst()
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
