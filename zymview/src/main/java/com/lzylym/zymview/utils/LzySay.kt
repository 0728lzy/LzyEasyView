package com.lzylym.zymview.utils

import android.content.Context
import android.widget.Toast

object LzySay {
    fun sayHelloToLYM(context: Context){
        Toast.makeText(context,"(*´▽｀)ノノ你好啊，小Y", Toast.LENGTH_SHORT).show()
    }

    fun sayGoodNightToLYM(context: Context){
        Toast.makeText(context,"晚安，笨蛋~", Toast.LENGTH_SHORT).show()
    }
}