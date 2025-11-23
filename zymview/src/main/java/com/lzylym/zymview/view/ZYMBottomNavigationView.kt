package com.lzylym.zymview.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Nullable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lzylym.zymview.R

class ZYMBottomNavigationView @JvmOverloads constructor(
    context: Context,
    @Nullable attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {

    private var customMaxItemCount: Int = 7

    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ZYMBottomNavigationView)
            customMaxItemCount = typedArray.getInt(
                R.styleable.ZYMBottomNavigationView_zym_max_item_count,
                7
            )
            typedArray.recycle()
        }
    }

    override fun getMaxItemCount(): Int {
        return customMaxItemCount
    }

    fun setZymMaxItemCount(count: Int) {
        this.customMaxItemCount = count
    }
}