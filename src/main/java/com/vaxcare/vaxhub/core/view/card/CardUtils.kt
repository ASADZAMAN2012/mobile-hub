/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.card

object CardUtils {
    fun getPossibleCardBrand(cardNumber: String?): CardBrand {
        return getPossibleCardBrand(cardNumber, true)
    }

    fun separateCardNumberGroups(spacelessCardNumber: String, brand: CardBrand): Array<String?> {
        return if (brand == CardBrand.AmericanExpress) {
            separateAmexCardNumberGroups(spacelessCardNumber.take(16))
        } else {
            separateDefaultCardNumberGroups(spacelessCardNumber.take(16))
        }
    }

    private fun separateDefaultCardNumberGroups(spacelessCardNumber: String): Array<String?> {
        val numberGroups = arrayOfNulls<String?>(4)
        var i = 0
        var previousStart = 0
        while ((i + 1) * 4 < spacelessCardNumber.length) {
            val group = spacelessCardNumber.substring(previousStart, (i + 1) * 4)
            numberGroups[i] = group
            previousStart = (i + 1) * 4
            i++
        }
        numberGroups[i] = spacelessCardNumber.substring(previousStart)

        return numberGroups
    }

    private fun separateAmexCardNumberGroups(spacelessCardNumber: String): Array<String?> {
        val numberGroups = arrayOfNulls<String?>(3)

        val length = spacelessCardNumber.length
        var lastUsedIndex = 0
        if (length > 4) {
            numberGroups[0] = spacelessCardNumber.substring(0, 4)
            lastUsedIndex = 4
        }

        if (length > 10) {
            numberGroups[1] = spacelessCardNumber.substring(4, 10)
            lastUsedIndex = 10
        }

        for (i in 0..2) {
            if (numberGroups[i] != null) {
                continue
            }
            numberGroups[i] = spacelessCardNumber.substring(lastUsedIndex)
            break
        }

        return numberGroups
    }

    fun removeSpacesAndHyphens(cardNumberWithSpaces: String?): String? {
        return cardNumberWithSpaces
            .takeUnless { it.isNullOrBlank() }
            ?.replace("\\s|-".toRegex(), "")
    }

    fun isValidCardNumber(cardNumber: String?): Boolean {
        val normalizedNumber = removeSpacesAndHyphens(cardNumber)
        return isValidLuhnNumber(normalizedNumber) && isValidCardLength(normalizedNumber)
    }

    private fun isValidExpirationMonth(month: String?): Boolean {
        return try {
            month?.toInt() ?: 0 in 1..12
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun isValidExpirationYear(year: String?): Boolean {
        return try {
            year?.length == 2
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun isValidCardLength(cardNumber: String?): Boolean {
        return cardNumber != null &&
            getPossibleCardBrand(cardNumber, false).isValidCardNumberLength(cardNumber)
    }

    private fun getPossibleCardBrand(cardNumber: String?, shouldNormalize: Boolean): CardBrand {
        if (cardNumber.isNullOrBlank()) {
            return CardBrand.Unknown
        }

        val spacelessCardNumber = if (shouldNormalize) {
            removeSpacesAndHyphens(cardNumber)
        } else {
            cardNumber
        }

        return CardBrand.fromCardNumber(spacelessCardNumber)
    }

    private fun isValidLuhnNumber(number: String?): Boolean {
        if (number == null) {
            return false
        }

        var isOdd = true
        var sum = 0

        for (index in number.length - 1 downTo 0) {
            val c = number[index]
            if (!Character.isDigit(c)) {
                return false
            }

            isOdd = !isOdd

            var digitInteger = Character.getNumericValue(c)
            if (isOdd) {
                digitInteger *= 2
            }

            if (digitInteger > 9) {
                digitInteger -= 9
            }

            sum += digitInteger
        }
        return sum % 10 == 0
    }
}
