package com.lzylym.zymview.utils

object JudgeValidUtils {
    fun isValidPhoneNumber(phone: String): Boolean {
        val regex = Regex("^1[3-9]\\d{9}$")
        return regex.matches(phone)
    }

    fun isValidEmail(email: String): Boolean {
        val regex = Regex(
            "^(?!.*\\.\\.)" +                     // 不允许连续点
                    "[A-Za-z0-9+_.-]+(?<!\\.)" +         // 用户名部分，不以点结尾
                    "@" +
                    "([A-Za-z0-9-]+\\.)+" +              // 域名部分，可以有子域名
                    "[A-Za-z]{2,}$"                       // 顶级域名，至少 2 个字母
        )
        return regex.matches(email)
    }
}