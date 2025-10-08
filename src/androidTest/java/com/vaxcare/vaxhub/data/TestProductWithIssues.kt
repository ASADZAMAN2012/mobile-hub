/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.enums.Gender
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.ProductStatus
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.inventory.AgeIndication
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product
import java.time.LocalDate

object TestProductWithIssues {
    val ExpiredProduct = LotNumberWithProduct(
        expirationDate = LocalDate.parse("2019-10-15"),
        id = 1269321,
        name = "MMRTEST",
        productId = 26,
        salesLotNumberId = 1269321,
        salesProductId = 26,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 26, 9999999, 180, 26, null)
        ),
        cptCodes = emptyList(),
        product = Product(
            antigen = "MMR",
            categoryId = ProductCategory.VACCINE,
            description = "Measles, Mumps, and Rubella Virus vaccine Live ",
            displayName = "MMR (MMR II)",
            id = 26,
            inventoryGroup = "MMR",
            lossFee = 8388,
            productNdc = "0006468101",
            routeCode = RouteCode.SC,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = 8388,
            visDates = "MMR: 2018-02-12",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "MMR (MMR II)"
        )
    )

    val MedDProduct = LotNumberWithProduct(
        expirationDate = LocalDate.parse("2023-04-01"), id = 1269312, name = "SHINGRIXTEST1",
        productId = 212, salesLotNumberId = 1269312, salesProductId = 212,
        ageIndications = listOf(
            AgeIndication(
                gender = Gender.MF,
                id = 30049,
                maxAge = 99999,
                minAge = 18250,
                productId = 212,
                warning = null
            )
        ),
        cptCodes = emptyList(),
        product = Product(
            antigen = "Zoster",
            categoryId = ProductCategory.VACCINE,
            description = "Shingrix SDV (Age 50 years and older)",
            displayName = "Zoster (Shingrix)",
            id = 212,
            inventoryGroup = "Zoster",
            lossFee = 17317,
            productNdc = "5816082311",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = 17317,
            visDates = "Zoster: 2018-02-12",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Zoster (Shingrix)"
        )
    )

    val OutOfAgeIndicated = LotNumberWithProduct(
        expirationDate = LocalDate.parse("2022-05-09"),
        id = 1269516,
        name = "QUADTEST",
        productId = 167,
        salesLotNumberId = 1269516,
        salesProductId = 180,
        ageIndications = listOf(
            AgeIndication(
                gender = Gender.MF,
                id = 30009,
                maxAge = 99999,
                minAge = 99999,
                productId = 167,
                warning = null
            )
        ),
        cptCodes = emptyList(),
        product = Product(
            antigen = "DTaP-IPV",
            categoryId = ProductCategory.VACCINE,
            description = "Quadracel 10x1 SDV (0.5 mL)",
            displayName = "DTaP-IPV (Quadracel SDV)",
            id = 167,
            inventoryGroup = "IPV + DTaP",
            lossFee = 4566,
            productNdc = "4928156258",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = 4566,
            visDates = "DTaP: 2007-05-17, IPV: 2011-11-08",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "DTaP-IPV (Quadracel SDV)"
        )
    )
}
