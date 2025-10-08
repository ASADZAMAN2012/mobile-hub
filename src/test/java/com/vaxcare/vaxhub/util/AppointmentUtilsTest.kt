package com.vaxcare.vaxhub.util

import com.vaxcare.vaxhub.util.AppointmentUtils.getNextAppointmentSlot
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AppointmentUtilsTest {
    @Test
    fun `getNextAppointmentSlot returns next 15 minute slot when current minute is less than 15`() {
        val currentDateTime = LocalDateTime.of(2023, 10, 26, 10, 10, 0)
        val expectedDateTime = LocalDateTime.of(2023, 10, 26, 10, 15, 0)

        val actualDateTime = currentDateTime.getNextAppointmentSlot()

        assertEquals(expectedDateTime, actualDateTime)
    }

    @Test
    fun `getNextAppointmentSlot returns next 30 minute slot when current minute is between 15 and 29`() {
        val currentDateTime = LocalDateTime.of(2023, 10, 26, 10, 20, 0)
        val expectedDateTime = LocalDateTime.of(2023, 10, 26, 10, 30, 0)

        val actualDateTime = currentDateTime.getNextAppointmentSlot()

        assertEquals(expectedDateTime, actualDateTime)
    }

    @Test
    fun `getNextAppointmentSlot returns next 45 minute slot when current minute is between 30 and 44`() {
        val currentDateTime = LocalDateTime.of(2023, 10, 26, 10, 35, 0)
        val expectedDateTime = LocalDateTime.of(2023, 10, 26, 10, 45, 0)

        val actualDateTime = currentDateTime.getNextAppointmentSlot()

        assertEquals(expectedDateTime, actualDateTime)
    }

    @Test
    fun `getNextAppointmentSlot returns next hour slot when current minute is between 45 and 59`() {
        val currentDateTime = LocalDateTime.of(2023, 10, 26, 10, 50, 0)
        val expectedDateTime = LocalDateTime.of(2023, 10, 26, 11, 0, 0)

        val actualDateTime = currentDateTime.getNextAppointmentSlot()

        assertEquals(expectedDateTime, actualDateTime)
    }

    @Test
    fun `getNextAppointmentSlot removes seconds from the resulting time`() {
        val currentDateTime = LocalDateTime.of(2023, 10, 26, 10, 10, 30)
        val expectedDateTime = LocalDateTime.of(2023, 10, 26, 10, 15, 0)

        val actualDateTime = currentDateTime.getNextAppointmentSlot()

        assertEquals(expectedDateTime, actualDateTime)
    }
}
