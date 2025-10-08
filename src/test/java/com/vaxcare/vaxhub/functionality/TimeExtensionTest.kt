/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.extension.getRelativeDay
import com.vaxcare.vaxhub.core.extension.isBetween
import com.vaxcare.vaxhub.core.extension.toStandardDate
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate

class TimeExtensionTest {
    @Test
    fun standardDateTest() {
        val bday = LocalDate.parse("1989-11-21")
        val xMas = LocalDate.parse("0000-12-25")
        val indDay = LocalDate.parse("1776-07-04")

        // "November 21st, 1989"
        assert(bday.toStandardDate() == "November 21st, 1989")

        // "December 25th, 0001" - for some reason the year 0000 does not exist according to scientists
        assert(xMas.toStandardDate() == "December 25th, 0001")

        // "July 4th, 1776"
        assert(indDay.toStandardDate() == "July 4th, 1776")
    }

    @Test
    fun relativeDateTest() {
        val today = LocalDate.now()
        val yesterday = LocalDate.now().minusDays(1)
        val tomorrow = LocalDate.now().plusDays(1)
        val dayAfter = LocalDate.now().plusDays(2)
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        Assert.assertEquals("Today", today.getRelativeDay())
        Assert.assertEquals("Yesterday", yesterday.getRelativeDay())
        Assert.assertEquals("Tomorrow", tomorrow.getRelativeDay())
        Assert.assertTrue(dayNames.contains(dayAfter.getRelativeDay()))
    }

    @Test
    fun betweenTest() {
        val today = LocalDate.now()
        val (lastMonth, nextMonth, nextYear) =
            today.minusMonths(1) to today.plusMonths(1) to today.plusYears(1)

        // today + 3 days between last month and next month true
        assert(today.plusDays(3).isBetween(lastMonth, nextMonth))

        // today - 100 days between last month and next year false
        assert(!today.minusDays(100).isBetween(lastMonth, nextYear))

        // today + 200 days between next month and next year true
        assert(today.plusDays(200).isBetween(nextMonth, nextYear))
    }

    private infix fun <A, B, C> Pair<A, B>.to(third: C) = Triple(first, second, third)
}
