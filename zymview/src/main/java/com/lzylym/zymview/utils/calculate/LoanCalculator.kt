package com.lzylym.zymview.utils.calculate

import java.math.BigDecimal
import java.math.RoundingMode

object LoanCalculator {
    fun calculate(
        amount: Double,
        years: Int,
        rateYearly: Double,
        method: LoanMethod
    ): LoanResult {
        val principalTotal = BigDecimal(amount)
        val months = years * 12
        val rateMonthly = BigDecimal(rateYearly)
            .divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
            .divide(BigDecimal(12), 10, RoundingMode.HALF_UP)

        return when (method) {
            LoanMethod.EQUAL_PAYMENT -> calculateEqualPayment(principalTotal, months, rateMonthly)
            LoanMethod.EQUAL_PRINCIPAL -> calculateEqualPrincipal(principalTotal, months, rateMonthly)
        }
    }

    //等额本息
    private fun calculateEqualPayment(
        principal: BigDecimal,
        months: Int,
        rate: BigDecimal
    ): LoanResult {
        val schedule = ArrayList<PaymentItem>()
        val onePlusRatePow = (BigDecimal.ONE + rate).pow(months)
        val monthlyPaymentBig = principal.multiply(rate).multiply(onePlusRatePow)
            .divide(onePlusRatePow.subtract(BigDecimal.ONE), 10, RoundingMode.HALF_UP)

        var currentPrincipal = principal
        var totalInterest = BigDecimal.ZERO

        for (i in 1..months) {
            val interestMonth = currentPrincipal.multiply(rate).setScale(2, RoundingMode.HALF_UP)
            var principalMonth = monthlyPaymentBig.subtract(interestMonth).setScale(2, RoundingMode.HALF_UP)

            if (i == months) {
                principalMonth = currentPrincipal
            }

            val paymentMonth = principalMonth.add(interestMonth)

            currentPrincipal = currentPrincipal.subtract(principalMonth)
            totalInterest = totalInterest.add(interestMonth)

            schedule.add(
                PaymentItem(
                    index = i,
                    payment = paymentMonth.toDouble(),
                    principal = principalMonth.toDouble(),
                    interest = interestMonth.toDouble(),
                    remaining = currentPrincipal.max(BigDecimal.ZERO).toDouble()
                )
            )
        }

        return LoanResult(
            firstMonthPayment = schedule.firstOrNull()?.payment ?: 0.0,
            totalInterest = totalInterest.toDouble(),
            totalPayment = principal.add(totalInterest).toDouble(),
            schedule = schedule
        )
    }

    //等额本金
    private fun calculateEqualPrincipal(
        principal: BigDecimal,
        months: Int,
        rate: BigDecimal
    ): LoanResult {
        val schedule = ArrayList<PaymentItem>()
        val principalPerMonth = principal.divide(BigDecimal(months), 2, RoundingMode.HALF_UP)

        var currentPrincipal = principal
        var totalInterest = BigDecimal.ZERO
        var accumulatedPrincipal = BigDecimal.ZERO

        for (i in 1..months) {
            val interestMonth = currentPrincipal.multiply(rate).setScale(2, RoundingMode.HALF_UP)

            var realPrincipalMonth = principalPerMonth
            if (i == months) {
                realPrincipalMonth = principal.subtract(accumulatedPrincipal)
            }

            val paymentMonth = realPrincipalMonth.add(interestMonth)

            accumulatedPrincipal = accumulatedPrincipal.add(realPrincipalMonth)
            currentPrincipal = currentPrincipal.subtract(realPrincipalMonth)
            totalInterest = totalInterest.add(interestMonth)

            schedule.add(
                PaymentItem(
                    index = i,
                    payment = paymentMonth.toDouble(),
                    principal = realPrincipalMonth.toDouble(),
                    interest = interestMonth.toDouble(),
                    remaining = currentPrincipal.max(BigDecimal.ZERO).toDouble()
                )
            )
        }

        return LoanResult(
            firstMonthPayment = schedule.firstOrNull()?.payment ?: 0.0,
            totalInterest = totalInterest.toDouble(),
            totalPayment = principal.add(totalInterest).toDouble(),
            schedule = schedule
        )
    }
}

// 贷款方式枚举
enum class LoanMethod {
    EQUAL_PAYMENT,   // 等额本息
    EQUAL_PRINCIPAL  // 等额本金
}

// 每一期的还款详情
data class PaymentItem(
    val index: Int,             // 期数 (第几月)
    val payment: Double,        // 月供 (本金+利息)
    val principal: Double,      // 当月本金
    val interest: Double,       // 当月利息
    val remaining: Double       // 剩余贷款余额
)

// 最终返回的汇总结果
data class LoanResult(
    val firstMonthPayment: Double, // 首月月供
    val totalInterest: Double,     // 累计利息
    val totalPayment: Double,      // 累计还款总额
    val schedule: List<PaymentItem> // 还款计划表
)