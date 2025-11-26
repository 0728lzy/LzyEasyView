package com.lzylym.zymview.utils.calculate

import java.util.*

data class Festival(
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int
)

data class NextFestivalResult(
    val name: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val daysUntil: Long
)

object ChinaFestivals {

    private val festivals = mutableListOf<Festival>()

    init {
        val festivalData = mapOf(
            "元旦" to listOf(
                Triple(2026, 1, 1),
                Triple(2027, 1, 1),
                Triple(2028, 1, 1),
                Triple(2029, 1, 1),
                Triple(2030, 1, 1)
            ),
            "春节" to listOf(
                Triple(2026, 2, 17),
                Triple(2027, 2, 6),
                Triple(2028, 1, 26),
                Triple(2029, 2, 13),
                Triple(2030, 2, 3)
            ),
            "元宵节" to listOf(
                Triple(2026, 3, 6),
                Triple(2027, 2, 24),
                Triple(2028, 2, 14),
                Triple(2029, 2, 28),
                Triple(2030, 2, 18)
            ),
            "清明节" to listOf(
                Triple(2026, 4, 4),
                Triple(2027, 4, 4),
                Triple(2028, 4, 4),
                Triple(2029, 4, 4),
                Triple(2030, 4, 4)
            ),
            "劳动节" to listOf(
                Triple(2026, 5, 1),
                Triple(2027, 5, 1),
                Triple(2028, 5, 1),
                Triple(2029, 5, 1),
                Triple(2030, 5, 1)
            ),
            "端午节" to listOf(
                Triple(2026, 6, 19),
                Triple(2027, 6, 9),
                Triple(2028, 6, 28),
                Triple(2029, 6, 17),
                Triple(2030, 6, 6)
            ),
            "中秋节" to listOf(
                Triple(2026, 9, 25),
                Triple(2027, 9, 15),
                Triple(2028, 10, 3),
                Triple(2029, 9, 22),
                Triple(2030, 9, 12)
            ),
            "国庆节" to listOf(
                Triple(2026, 10, 1),
                Triple(2027, 10, 1),
                Triple(2028, 10, 1),
                Triple(2029, 10, 1),
                Triple(2030, 10, 1)
            ),
            "重阳节" to listOf(
                Triple(2026, 10, 15),
                Triple(2027, 10, 4),
                Triple(2028, 10, 22),
                Triple(2029, 10, 11),
                Triple(2030, 10, 31)
            )
        )

        festivalData.forEach { (name, dates) ->
            dates.forEach { (y, m, d) ->
                festivals.add(Festival(name, y, m, d))
            }
        }

        festivals.sortWith(compareBy({ it.year }, { it.month }, { it.day }))
    }

    fun getNextFestival(reference: Calendar = Calendar.getInstance()): NextFestivalResult? {
        reference.set(Calendar.HOUR_OF_DAY, 0)
        reference.set(Calendar.MINUTE, 0)
        reference.set(Calendar.SECOND, 0)
        reference.set(Calendar.MILLISECOND, 0)
        val refTime = reference.timeInMillis

        for (festival in festivals) {
            val cal = Calendar.getInstance()
            cal.set(festival.year, festival.month - 1, festival.day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val diff = cal.timeInMillis - refTime
            if (diff >= 0) {
                val days = diff / (24 * 60 * 60 * 1000)
                return NextFestivalResult(
                    festival.name,
                    festival.year,
                    festival.month,
                    festival.day,
                    days
                )
            }
        }
        return null
    }
}