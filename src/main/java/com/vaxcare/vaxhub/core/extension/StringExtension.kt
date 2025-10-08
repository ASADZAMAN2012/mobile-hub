/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import java.util.regex.Pattern
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object REGEX {
    // example valid MBIs: 1EG4TE5MK79 and 1AA1AA1AA11
    const val MBI_FORMAT =
        "[1-9]([A-Z&&[^SLOIBZ]][0-9A-Z&&[^SLOIBZ]]\\d){2}[A-Z&&[^SLOIBZ]]{2}\\d{2}"
    const val MASKED_SSN_FORMAT = "^((X|\\d){3}-?(X|\\d){2}-?(\\d{4}\$))"
    const val SSN_FORMAT = "^(\\d{3}-?\\d{2}-?(\\d{4}$))"
    const val SSN_PARTIAL = "\\d*"
    const val ALPHA_NUMERIC = "[^a-zA-Z0-9]*"
}

/**
 * Capitalize first letter
 *
 * @return
 */
fun String.captureName(): String {
    if (this.isEmpty()) {
        return this
    }
    if (this.length == 1) {
        return this.uppercase()
    }
    return this.substring(0, 1).uppercase() + this.substring(1).lowercase()
}

/**
 * Remove the special characters and display only alphanumeric characters.
 * - No dashes, no hashes, no crashes or slashes, no spaces, whatever.
 *
 * @return the string only alphanumeric characters
 */
fun String.toAlphanumeric(): String = this.replace(REGEX.ALPHA_NUMERIC.toRegex(), "")

fun String.trimDashes() = replace("-", "")

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isMbiFormatAndNotNull(): Boolean {
    contract {
        returns(true) implies (this@isMbiFormatAndNotNull != null)
    }

    return this?.let {
        REGEX.MBI_FORMAT.toPattern(Pattern.CASE_INSENSITIVE)
            .matcher(it.replace("-".toRegex(), "")).matches()
    } ?: false
}

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isFullSsnAndNotNull(): Boolean {
    contract {
        returns(true) implies (this@isFullSsnAndNotNull != null)
    }

    return this?.let {
        REGEX.SSN_FORMAT.toPattern(Pattern.CASE_INSENSITIVE).matcher(it).matches()
    } ?: false
}

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isMaskedSsnFormatted(): Boolean {
    contract {
        returns(true) implies (this@isMaskedSsnFormatted != null)
    }

    return this?.let {
        REGEX.MASKED_SSN_FORMAT.toPattern(Pattern.CASE_INSENSITIVE).matcher(it).matches()
    } ?: false
}

fun CharSequence?.isPartialSsn() =
    this?.let {
        REGEX.SSN_PARTIAL.toPattern(Pattern.CASE_INSENSITIVE).matcher(it).matches()
    } ?: false

fun CharSequence?.isAlphaNumeric() =
    this?.let {
        REGEX.ALPHA_NUMERIC.toPattern(Pattern.CASE_INSENSITIVE).matcher(it).matches()
    } ?: false

fun String.formatPhoneNumber(): String {
    if (length != 10) {
        return this
    }
    return "${substring(0, 3)}-${substring(3, 6)}-${substring(6, 10)}"
}

fun String.maskSSNIfHasNineDigits(): String {
    val ssnWithoutDash = this.replace("-", "")
    if (ssnWithoutDash.length != 9) {
        return this
    }
    return "XXX-XX-${ssnWithoutDash.substring(5)}"
}
