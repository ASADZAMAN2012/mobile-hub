/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.card

sealed class CardBrand(
    val type: String,
    // e.g. "4242 4242 4242 4242"
    val maxLengthWithSpaces: Int = 19,
    // e.g. "4242424242424242"
    val maxLengthWithoutSpaces: Int = 16,
    val maxLengthWithoutSpacesAlternate: Int = 16,
    // Based on http://en.wikipedia.org/wiki/Bank_card_number#Issuer_identification_number_.28IIN.29
    val prefixes: List<String> = emptyList(),
    val spacePositions: Set<Int> = setOf(4, 9, 14),
    val regex: Regex = "".toRegex()
) {
    object AmericanExpress : CardBrand(
        "Amex",
        maxLengthWithSpaces = 17,
        maxLengthWithoutSpaces = 15,
        maxLengthWithoutSpacesAlternate = 15,
        prefixes = listOf("34", "37"),
        spacePositions = setOf(4, 11),
        regex = "^3[47][0-9]{5,}$".toRegex()
    )

    object Visa : CardBrand(
        "Visa",
        prefixes = listOf("4"),
        regex = "^4[0-9]{6,}$".toRegex()
    )

    object MasterCard : CardBrand(
        "Mastercard",
        prefixes = listOf(
            "2221", "2222", "2223", "2224", "2225", "2226", "2227", "2228", "2229", "223", "224",
            "225", "226", "227", "228", "229", "23", "24", "25", "26", "270", "271", "2720",
            "50", "51", "52", "53", "54", "55", "67"
        ),
        regex = "^[52][1-5][0-9]{5,}$".toRegex()
    )

    object Discover : CardBrand(
        "Discover",
        prefixes = listOf("6011", "644-649", "65"),
        maxLengthWithoutSpaces = 16,
        maxLengthWithoutSpacesAlternate = 15,
        regex = "^6(?:011|5[0-9]{2})[0-9]{3,}$".toRegex()
    )

    object Unknown : CardBrand(
        "Unknown"
    )

    fun isValidCardNumberLength(cardNumber: String?): Boolean {
        return cardNumber != null && Unknown != this &&
            (
                cardNumber.length == maxLengthWithoutSpaces ||
                    cardNumber.length == maxLengthWithoutSpacesAlternate
            )
    }

    companion object {
        fun fromCardNumber(cardNumber: String?): CardBrand =
            CardBrand::class.sealedSubclasses.mapNotNull { it.objectInstance }
                .firstOrNull { cardBrand ->
                    cardBrand.regex.matches(cardNumber ?: "")
                } ?: Unknown
    }
}
