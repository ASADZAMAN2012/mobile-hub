/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.R

sealed class TestProducts(
    val id: Int,
    val antigen: String,
    val productName: String,
    val antigenProductName: String,
    val lotNumber: String,
    val expiryYear: String = (java.time.Year.now().value + 1).toString(),
    val expiryMonth: String = "Dec",
    val expiryDay: String = "10",
    val presentation: String = "Single Dose Vial",
    val hasCopay: Boolean = false,
    val icon: Int = 0
) {
    object Dtap : TestProducts(
        id = 92,
        antigen = "DTaP",
        productName = "Infanrix",
        antigenProductName = "DTaP (Infanrix)",
        lotNumber = "M4ZE7"
    )

    object Varicella : TestProducts(
        id = 13,
        antigen = "Varicella",
        productName = "Varivax",
        antigenProductName = "Varicella (Varivax)",
        lotNumber = "J003535",
        icon = R.drawable.ic_presentation_single_dose_vial
    )

    object Adacel : TestProducts(
        id = 17,
        antigen = "Tdap",
        productName = "Adacel",
        antigenProductName = "Tdap (Adacel)",
        hasCopay = true,
        lotNumber = "ADACELU7803AA"
    )

    object PPSV23 : TestProducts(
        id = 113,
        antigen = "PPSV23",
        productName = "Pneumovax23",
        antigenProductName = "PPSV23 (Pneumovax23)",
        lotNumber = "TO39322"
    )

    object Shingrix : TestProducts(
        id = 212,
        antigen = "Zoster",
        productName = "Shingrix",
        antigenProductName = "Zoster (Shingrix)",
        lotNumber = "ZOSTE",
        hasCopay = true
    )

    object Boostrix : TestProducts(
        id = 82,
        antigen = "Tdap",
        productName = "Boostrix",
        antigenProductName = "Tdap (Boostrix)",
        lotNumber = "3JS9E",
        hasCopay = true
    )

    object RSV : TestProducts(
        id = 82,
        antigen = "RSV",
        productName = "Abrysvo",
        antigenProductName = "Abrysvo",
        lotNumber = "RSV",
        hasCopay = true
    )

    object IPV : TestProducts(
        id = 8,
        antigen = "IPV",
        productName = "IPOL",
        antigenProductName = "IPV (IPOL)",
        lotNumber = "Y1D49P1"
    )

    object IPOL : TestProducts(
        id = 8,
        antigen = "IPV",
        productName = "IPOL",
        antigenProductName = "IPV (IPOL)",
        lotNumber = "V1B90",
        icon = R.drawable.ic_presentation_multi_dose_vial
    )

    object ModernaCovid : TestProducts(
        id = 329,
        antigen = "COVID",
        productName = "MDV",
        antigenProductName = "COVID (Moderna COVID Bivalent)",
        lotNumber = "055F22B",
        hasCopay = false,
        icon = R.drawable.ic_presentation_single_dose_vial
    )
}
