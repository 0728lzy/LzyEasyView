package com.lzylym.zymview.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.lzylym.zymview.R

class ZYMExpandableQAView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val headerLayout: LinearLayout
    private val contentLayout: LinearLayout
    private val tvQuestion: TextView
    private val tvAnswer: TextView
    private val ivArrow: ImageView
    private val divider: View
    private var expanded = false
    private var animType = 1

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_zym_expandable_qa, this, true)

        headerLayout = findViewById(R.id.headerLayout)
        contentLayout = findViewById(R.id.contentLayout)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvAnswer = findViewById(R.id.tvAnswer)
        ivArrow = findViewById(R.id.ivArrow)
        divider = findViewById(R.id.divider)

        val a = context.obtainStyledAttributes(attrs, R.styleable.ZYMExpandableQAView)

        animType = a.getInt(R.styleable.ZYMExpandableQAView_qa_expandAnimType, 1)

        tvQuestion.text = a.getString(R.styleable.ZYMExpandableQAView_qa_questionText) ?: "问题"
        tvAnswer.text = a.getString(R.styleable.ZYMExpandableQAView_qa_answerText) ?: "这是答案"

        val qSize = a.getDimension(R.styleable.ZYMExpandableQAView_qa_questionTextSize, dp(16f))
        val qColor = a.getColor(R.styleable.ZYMExpandableQAView_qa_questionTextColor, ContextCompat.getColor(context, android.R.color.black))
        val qStyle = a.getInt(R.styleable.ZYMExpandableQAView_qa_questionTextStyle, 0)

        val aSize = a.getDimension(R.styleable.ZYMExpandableQAView_qa_answerTextSize, dp(14f))
        val aColor = a.getColor(R.styleable.ZYMExpandableQAView_qa_answerTextColor, ContextCompat.getColor(context, android.R.color.darker_gray))
        val aStyle = a.getInt(R.styleable.ZYMExpandableQAView_qa_answerTextStyle, 0)

        val iconSrc = a.getResourceId(R.styleable.ZYMExpandableQAView_qa_iconSrc, android.R.drawable.arrow_down_float)
        val iconWidth = a.getDimensionPixelSize(R.styleable.ZYMExpandableQAView_qa_iconWidth, dpInt(20f))
        val iconHeight = a.getDimensionPixelSize(R.styleable.ZYMExpandableQAView_qa_iconHeight, dpInt(20f))
        val iconTint = a.getColor(R.styleable.ZYMExpandableQAView_qa_iconTint, ContextCompat.getColor(context, android.R.color.black))

        val dividerHeight = a.getDimensionPixelSize(R.styleable.ZYMExpandableQAView_qa_dividerHeight, dpInt(1f))
        val dividerMarginH = a.getDimensionPixelSize(R.styleable.ZYMExpandableQAView_qa_dividerMarginHorizontal, dpInt(0f))
        val dividerMarginV = a.getDimensionPixelSize(R.styleable.ZYMExpandableQAView_qa_dividerMarginVertical, dpInt(5f))
        val dividerColor = a.getColor(R.styleable.ZYMExpandableQAView_qa_dividerColor, 0xFFDDDDDD.toInt())

        a.recycle()

        setQuestionTextSizePx(qSize)
        setQuestionTextColor(qColor)
        setQuestionTextStyle(qStyle)

        setAnswerTextSizePx(aSize)
        setAnswerTextColor(aColor)
        setAnswerTextStyle(aStyle)

        setArrowIcon(iconSrc)
        setArrowSize(iconWidth, iconHeight)
        setArrowTint(iconTint)

        setDividerHeight(dividerHeight)
        setDividerMarginHorizontal(dividerMarginH)
        setDividerMarginVertical(dividerMarginV)
        setDividerColor(dividerColor)

        headerLayout.setOnClickListener { toggle() }
    }

    private fun toggle() {
        expanded = !expanded
        animateArrow(if (expanded) 270f else 360f, if (expanded) 360f else 270f)
        if (expanded) {
            if (animType == 0) fadeExpand(contentLayout) else slideExpand(contentLayout)
        } else {
            if (animType == 0) fadeCollapse(contentLayout) else slideCollapse(contentLayout)
        }
    }

    private fun fadeExpand(v: View) {
        v.alpha = 0f
        v.visibility = View.VISIBLE
        v.animate().alpha(1f).setDuration(200).start()
    }

    private fun fadeCollapse(v: View) {
        v.animate().alpha(0f).setDuration(200).withEndAction { v.visibility = View.GONE }.start()
    }

    private fun slideExpand(view: View) {
        view.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        val targetHeight = view.measuredHeight
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 200
        animator.start()
    }

    private fun slideCollapse(view: View) {
        val initialHeight = view.measuredHeight
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener {
            view.layoutParams.height = it.animatedValue as Int
            view.requestLayout()
        }
        animator.duration = 200
        animator.start()
        animator.doOnEnd { view.visibility = View.GONE }
    }

    private fun animateArrow(from: Float, to: Float) {
        val animator = ObjectAnimator.ofFloat(ivArrow, "rotation", from, to)
        animator.duration = 200
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    fun setQuestionText(text: String) { tvQuestion.text = text }
    fun setAnswerText(text: String) { tvAnswer.text = text }
    fun setQuestionTextSizePx(px: Float) { tvQuestion.textSize = pxToSp(px) }
    fun setQuestionTextColor(color: Int) { tvQuestion.setTextColor(color) }
    fun setQuestionTextStyle(style: Int) { tvQuestion.setTypeface(null, style) }

    fun setAnswerTextSizePx(px: Float) { tvAnswer.textSize = pxToSp(px) }
    fun setAnswerTextColor(color: Int) { tvAnswer.setTextColor(color) }
    fun setAnswerTextStyle(style: Int) { tvAnswer.setTypeface(null, style) }

    fun setArrowIcon(resId: Int) { ivArrow.setImageResource(resId) }
    fun setArrowSize(width: Int, height: Int) {
        ivArrow.layoutParams.width = width
        ivArrow.layoutParams.height = height
        ivArrow.requestLayout()
    }
    fun setArrowTint(color: Int) { ivArrow.setColorFilter(color) }

    fun setDividerHeight(height: Int) {
        val lp = divider.layoutParams
        lp.height = height
        divider.layoutParams = lp
    }
    fun setDividerMarginHorizontal(margin: Int) {
        val lp = divider.layoutParams as MarginLayoutParams
        lp.marginStart = margin
        lp.marginEnd = margin
        divider.layoutParams = lp
    }

    fun setDividerMarginVertical(margin: Int) {
        val lp = divider.layoutParams as MarginLayoutParams
        lp.topMargin = margin
        lp.bottomMargin = margin
        divider.layoutParams = lp
    }
    fun setDividerColor(color: Int) { divider.setBackgroundColor(color) }

    private fun dp(value: Float) = value * resources.displayMetrics.density
    private fun dpInt(value: Float) = (value * resources.displayMetrics.density).toInt()
    private fun pxToSp(px: Float) = px / resources.displayMetrics.scaledDensity
}
