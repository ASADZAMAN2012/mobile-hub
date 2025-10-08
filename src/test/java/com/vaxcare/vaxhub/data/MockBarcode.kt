/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import java.time.LocalDate

enum class MockBarcode(
    val value: String,
    val lotNumber: String? = null,
    val expirationDate: LocalDate? = null
) {
    NORMAL_BARCODE(
        value = "01103195158104151725061710J4349",
        lotNumber = "J4349",
        expirationDate = LocalDate.parse("2025-06-17")
    ),
    EXPIRATION_IN_WRONG_LOCATION(
        value = "01003660193111012106267998009470\u001D1724121610WF2584",
        lotNumber = "WF2584",
        expirationDate = LocalDate.parse("2024-12-16")
    ),
    UNKNOWN_ABNORMALITY(
        value = "01003195158105241725063010P324P\u001D215NX7CR2R80",
        lotNumber = "P324P",
        expirationDate = LocalDate.parse("2025-06-30")
    ),
    LOT_BEFORE_EXPIRATION(
        value = "010034928142488510UT8415JA\u001D17250630",
        lotNumber = "UT8415JA",
        expirationDate = LocalDate.parse("2025-06-30")
    ),
    STARTING_DIGITS(value = "00006490676W013059"),
}
