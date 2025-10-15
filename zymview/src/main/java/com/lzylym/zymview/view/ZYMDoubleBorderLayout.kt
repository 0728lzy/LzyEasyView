package com.lzylym.zymview.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.lzylym.zymview.R

class ZYMDoubleBorderLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val internalRoot: View
    private val content: LinearLayout

    private val border1: View
    private val border2: View

    @DrawableRes
    private var border1Res: Int = 0

    @DrawableRes
    private var border2Res: Int = 0

    init {
        val inflater = LayoutInflater.from(context)
        internalRoot = inflater.inflate(R.layout.layout_double_border, this, false)
        super.addView(internalRoot, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        content = internalRoot.findViewById(R.id.content)
            ?: throw IllegalStateException("layout_double_border must contain LinearLayout with id @id/content")

        border1 = internalRoot.findViewById(R.id.bg_helper_bord_1)
            ?: throw IllegalStateException("layout_double_border must contain View with id @id/bg_helper_bord_1")

        border2 = internalRoot.findViewById(R.id.bg_helper_bord_2)
            ?: throw IllegalStateException("layout_double_border must contain View with id @id/bg_helper_bord_2")

        val ta = context.obtainStyledAttributes(attrs, R.styleable.ZYMDoubleBorderLayout)
        border1Res = ta.getResourceId(R.styleable.ZYMDoubleBorderLayout_borderBackground1, 0)
        border2Res = ta.getResourceId(R.styleable.ZYMDoubleBorderLayout_borderBackground2, 0)
        ta.recycle()

        if (border1Res != 0) border1.setBackgroundResource(border1Res)
        if (border2Res != 0) border2.setBackgroundResource(border2Res)
    }

    fun setBorder1Background(@DrawableRes resId: Int) {
        border1Res = resId
        border1.setBackgroundResource(resId)
    }

    fun setBorder2Background(@DrawableRes resId: Int) {
        border2Res = resId
        border2.setBackgroundResource(resId)
    }

    fun getContentLayout(): LinearLayout = content

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child == null) return
        if (child === internalRoot || child === content) {
            super.addView(child, index, params)
            return
        }

        val newLp = if (params is MarginLayoutParams) {
            LinearLayout.LayoutParams(params.width, params.height).apply {
                leftMargin = params.leftMargin
                topMargin = params.topMargin
                rightMargin = params.rightMargin
                bottomMargin = params.bottomMargin
            }
        } else {
            LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        content.addView(child, index.coerceAtLeast(0), newLp)
    }

    override fun addView(child: View?) = addView(child, -1, child?.layoutParams)
    override fun addView(child: View?, params: ViewGroup.LayoutParams?) = addView(child, -1, params)
    override fun addView(child: View?, index: Int) = addView(child, index, child?.layoutParams)
}
