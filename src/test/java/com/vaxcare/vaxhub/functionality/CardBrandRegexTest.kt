/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.core.view.card.CardBrand
import org.junit.Test

class CardBrandRegexTest {
    private val testCards = listOf(
        TestCard(
            number = "4111111111111111",
            type = "Visa"
        ),
        TestCard(
            number = "2222405343248877",
            type = "MasterCard"
        ),
        TestCard(
            number = "371449635398431",
            type = "Amex"
        ),
        TestCard(
            number = "6011111111111117",
            type = "Discover"
        ),
        TestCard(
            number = "9999999991238755",
            type = "Unknown"
        ),
        TestCard(
            number = "4012888888881881",
            type = "Visa"
        ),
        TestCard(
            number = "5506900490000436",
            type = "MasterCard"
        ),
        TestCard(
            number = "378282246310005",
            type = "Amex"
        ),
        TestCard(
            number = "6011000990139424",
            type = "Discover"
        ),
        TestCard(
            number = "1234notValidCard1234",
            type = "Unknown"
        )
    )

    private data class TestCard(
        val number: String,
        val type: String
    )

    @Test
    fun cardBrandRegexTest() {
        testCards.forEach { card ->
            val result = CardBrand.fromCardNumber(card.number)
            when (card.type) {
                "Visa" -> assert(result == CardBrand.Visa)
                "MasterCard" -> assert(result == CardBrand.MasterCard)
                "Amex" -> assert(result == CardBrand.AmericanExpress)
                "Discover" -> assert(result == CardBrand.Discover)
                "Unknown" -> assert(result == CardBrand.Unknown)
            }
        }
    }
}
