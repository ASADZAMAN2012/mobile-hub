/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun Instant.toLocalDateTime(pattern: String = "M/dd/yyyy h:mm a"): String =
    LocalDateTime
        .ofInstant(this, ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern))

fun Instant.toLocalDate(pattern: String = "M/dd/yyyy"): String =
    LocalDateTime
        .ofInstant(this, ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern))

fun Instant.toLocalTime(pattern: String = "h:mm a"): String =
    LocalDateTime
        .ofInstant(this, ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern))

fun LocalDateTime.toLocalDateString(pattern: String = "M-dd-yyyy"): String =
    this.format(
        DateTimeFormatter.ofPattern(pattern)
    )

fun LocalDateTime.toLocalTimeString(pattern: String = "h:mm a"): String =
    this.format(
        DateTimeFormatter.ofPattern(pattern)
    )

fun LocalDate.toLocalDateString(pattern: String = "M/dd/yyyy"): String =
    this.format(
        DateTimeFormatter.ofPattern(pattern)
    )

fun LocalDate.getSuffixOfDay(): String {
    return when (this.dayOfMonth) {
        1, 21, 31 -> "st"
        2, 22 -> "nd"
        3, 23 -> "rd"
        else -> "th"
    }
}

fun LocalDateTime.getSuffixOfDay(): String {
    return this.toLocalDate().getSuffixOfDay()
}

fun LocalDate.getRelativeDay(): String {
    val today = LocalDate.now()
    return when {
        this == today -> "Today"
        this == today.plusDays(1) -> "Tomorrow"
        this == today.minusDays(1) -> "Yesterday"
        else -> this.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}

fun LocalDateTime.getRelativeDay(): String {
    return this.toLocalDate().getRelativeDay()
}

fun LocalDateTime.getStartOfDay(pattern: String = "yyyy-MM-dd"): LocalDateTime =
    LocalDate.parse(this.format(DateTimeFormatter.ofPattern(pattern))).atStartOfDay()

fun LocalDateTime.toOrdinalDate(splitter: String = " "): String {
    val pattern = "EEEE,'$splitter'MMM. d'${getSuffixOfDay()}'"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDateTime.toRelativeDayTime(splitter: String = " at "): String {
    val pattern = "'${getRelativeDay()}$splitter'h:mm a"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDate.toRelativeOrdinalDate(splitter: String = ", "): String {
    val pattern = "'${getRelativeDay()}$splitter'MMM. d'${getSuffixOfDay()}'"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDate.toOrdinalDate(): String {
    val pattern = "MMM d'${getSuffixOfDay()}', yyyy"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDate.isToday() = this.isEqual(LocalDate.now())

fun LocalDateTime.toMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
    ZonedDateTime.of(this, zoneId).toInstant().toEpochMilli()

fun LocalDateTime.toStandardDate(): String {
    val pattern = "MMMM d'${getSuffixOfDay()},' yyyy"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

fun LocalDateTime.toUtc(): LocalDateTime =
    this.atZone(ZoneId.systemDefault()).withZoneSameInstant(
        ZoneId.of("UTC")
    ).toLocalDateTime()

/**
 * Formats date to human readable. Ex: 11/21/1900 will be "November 21st, 1900"
 */
fun LocalDate.toStandardDate(): String {
    val pattern = "MMMM d'${getSuffixOfDay()},' yyyy"
    return this.format(DateTimeFormatter.ofPattern(pattern))
}

/**
 * Check if date falls between two input dates. Note the order of dates does not matter.
 *
 * @param date1 first date comparator
 * @param date2 second date comparator
 * @return true if date falls between incoming params
 */
fun LocalDate.isBetween(date1: LocalDate, date2: LocalDate): Boolean {
    val (d1, d2) = if (date2.isAfter(date1)) {
        date1 to date2
    } else {
        date2 to date1
    }
    return this.isAfter(d1) && this.isBefore(d2)
}

fun LocalDate.nextMidnight(): LocalDateTime = this.plusDays(1).atStartOfDay()
