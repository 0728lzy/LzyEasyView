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

    override fun getMaxItemCount(): Int {
        return 7
    }

}