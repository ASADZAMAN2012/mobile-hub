/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.typeadapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.vaxcare.core.extensions.toLocalDate
import com.vaxcare.vaxhub.core.annotation.LocalTime
import com.vaxcare.vaxhub.core.extension.toLocalDateTime
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class TimeAdapter {
    companion object {
        private val default = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * Standard Date/Time yyyy-MM-dd HH:mm:ss.SSS
         */
        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss.SSS
         */
        private val dateFormatter24HourExtended: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

        /**
         * Standard Date/Time yyyy-MM-dd'T'hh:mm:ss
         */
        private val dateLocalFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss
         */
        private val dateLocalFormatter24Hour: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss'Z'
         */
        private val dateLocalFormatter24HourZulu: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss.S
         */
        private val dateLocalFormatter24HourExtended: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss.SS
         */
        private val dateLocalFormatter24HourExtended2: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss.SS
         */
        private val dateLocalFormatter24HourExtended3: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS")

        /**
         * Standard Date/Time yyyy-MM-dd'T'HH:mm:ss.SS'Z'
         */
        private val dateLocalFormatter24HourExtended2Zulu: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

        /**
         * List of standard Defined Date/Time Formats
         */
        val standardDateFormats = listOf(
            default,
            dateFormatter,
            dateFormatter24HourExtended,
            dateLocalFormatter,
            dateLocalFormatter24Hour,
            dateLocalFormatter24HourZulu,
            dateLocalFormatter24HourExtended,
            dateLocalFormatter24HourExtended2,
            dateLocalFormatter24HourExtended3,
            dateLocalFormatter24HourExtended2Zulu
        )
    }

    @FromJson
    fun stringToInstantNullable(
        @LocalTime string: String?
    ) = if (string != null) {
        try {
            LocalDateTime.parse(string, dateFormatter).toInstant(ZoneOffset.UTC)
        } catch (e: DateTimeParseException) {
            try {
                LocalDateTime.parse(string, dateLocalFormatter).toInstant(ZoneOffset.UTC)
            } catch (e: DateTimeParseException) {
                OffsetDateTime.parse(string).toInstant()
            }
        }
    } else {
        null
    }

    @ToJson
    @LocalTime
    fun instantToStringNullable(instant: Instant?): String? = instant?.toLocalDateTime("yyyy-MM-dd hh:mm:ss")

    @FromJson
    fun stringToLocalDate(string: String?): LocalDate? =
        string?.let { value ->
            try {
                LocalDate.parse(string)
            } catch (e: java.lang.Exception) {
                standardDateFormats.forEach { format ->
                    value.toLocalDate(format)?.let { date ->
                        return date
                    }
                }
                null
            }
        }

    @ToJson
    fun localDateToString(date: LocalDate?): String? =
        date?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00.000"))

    @FromJson
    fun stringToLocalDateTime(string: String?): LocalDateTime? =
        string?.let { input ->
            var result: LocalDateTime? =
                try {
                    LocalDateTime.parse(
                        string,
                        DateTimeFormatter.ofPattern(constructPatternBasedOnDecimals(string))
                    )
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }

            if (result == null) {
                standardDateFormats.forEach fe@{
                    try {
                        result = LocalDateTime.parse(input, it)
                        return@fe
                    } catch (e: Exception) {
                        // do nothing
                    }
                }
            }

            result
        }

    @ToJson
    fun localDateTimeToString(date: LocalDateTime?): String? =
        date?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

    private fun constructPatternBasedOnDecimals(input: String): String {
        var pattern = "yyyy-MM-dd'T'HH:mm:ss.S*"
        try {
            val hasZulu = if (input.contains("Z")) {
                "'Z'"
            } else {
                ""
            }
            val new = input.replace("Z", "")
            val index = new.indexOf('.')
            if (index < 0) {
                return "${pattern.replace(".S*", "")}$hasZulu"
            }
            val length = new.substring(index).length - 1

            val num = if (length == 0) {
                ""
            } else {
                (0..length).mapIndexed { idx, _ ->
                    if (idx == 0) {
                        "."
                    } else {
                        "S"
                    }
                }.joinToString("")
            }
            pattern = pattern.replace(".S*", "$num$hasZulu")
        } catch (e: Exception) {
            Timber.e(e)
            pattern = pattern.replace("*", "")
        }

        return pattern
    }
}
