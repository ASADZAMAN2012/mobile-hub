/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.enums.Gender
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.ProductStatus
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.inventory.AgeIndication
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch
import java.math.BigDecimal
import java.time.LocalDate

object MockProducts {
    val pediatricTdapDose = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 100, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "Tdap",
            categoryId = ProductCategory.VACCINE,
            description = "pediatric tdap",
            displayName = "Tdap Pediatric",
            id = 1,
            inventoryGroup = "tdap",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = null,
            visDates = "Tdap: 2099-11-21",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Tdap Pediatric"
        )
    ).apply {
        oneTouch = ProductOneTouch(
            id = 0,
            productId = 0,
            selfPayRate = BigDecimal.valueOf(10.0)
        )
    }
    val pediatricTdapDoseWithZeroSelfPayRate = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 100, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "Tdap",
            categoryId = ProductCategory.VACCINE,
            description = "pediatric tdap",
            displayName = "Tdap Pediatric",
            id = 1,
            inventoryGroup = "tdap",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = null,
            visDates = "Tdap: 2099-11-21",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Tdap Pediatric"
        )
    ).apply {
        oneTouch = ProductOneTouch(
            id = 0,
            productId = 0,
            selfPayRate = BigDecimal.ZERO
        )
    }
    val zosterDoseOneHundredSelfPay = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 100, 1, 1, null)
        ),
        cptCodes = listOf(),
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
    ).apply {
        oneTouch = ProductOneTouch(
            id = 0,
            productId = 0,
            selfPayRate = BigDecimal.valueOf(100)
        )
        copay = ProductCopayInfo(
            antigen = MedDVaccines.ZOSTER,
            copay = BigDecimal.ONE
        )
    }
    val rsvDoseZeroCopay = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 900000, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "RSV",
            categoryId = ProductCategory.VACCINE,
            description = "rsv",
            displayName = "RSV",
            id = 1,
            inventoryGroup = "RSV",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = null,
            visDates = "RSV: 2099-11-21",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "RSV"
        )
    ).apply {
        copay = ProductCopayInfo(
            antigen = MedDVaccines.RSV,
            copay = BigDecimal.ZERO
        )
    }
    val tdapDoseTenCopay = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 900000, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "Tdap",
            categoryId = ProductCategory.VACCINE,
            description = "pediatric tdap",
            displayName = "Tdap Pediatric",
            id = 1,
            inventoryGroup = "tdap",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = null,
            visDates = "Tdap: 2099-11-21",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Tdap"
        )
    ).apply {
        copay = ProductCopayInfo(
            antigen = MedDVaccines.RSV,
            copay = BigDecimal.TEN
        )
    }
    val rsvDoseMissingCopay = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 900000, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "RSV",
            categoryId = ProductCategory.VACCINE,
            description = "rsv",
            displayName = "RSV",
            id = 1,
            inventoryGroup = "RSV",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = null,
            visDates = "RSV: 2099-11-21",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "RSV"
        )
    )
    val ipolDose = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 900000, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "IPV",
            categoryId = ProductCategory.VACCINE,
            description = "ipol",
            displayName = "IPOL",
            id = 1,
            inventoryGroup = "IPV",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = null,
            visDates = "Polio: 2019-10-30",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "IPOL"
        )
    )
    val varivaxDose = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 1,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 900000, 1, 1, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "Varicella",
            categoryId = ProductCategory.VACCINE,
            description = "varivax",
            displayName = "Varivax",
            id = 1,
            inventoryGroup = "Varicella",
            lossFee = null,
            productNdc = "123",
            routeCode = RouteCode.SC,
            presentation = ProductPresentation.SINGLE_DOSE_VIAL,
            purchaseOrderFee = null,
            visDates = "Varicella: 2019-08-15",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Varivax"
        )
    )

    val beyfortus = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 0,
        name = "TestProd",
        productId = 1,
        salesProductId = 364,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 730, 0, 364, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "RSV",
            categoryId = ProductCategory.VACCINE,
            description = "Beyfortus >= 11lbs Prefilled Syringe(s) (1.0 mL)",
            displayName = "RSV (Beyfortus >= 11lbs)",
            id = 364,
            inventoryGroup = "RSV - Pediatric (>= 11 lbs)",
            lossFee = 44000,
            productNdc = "N/A",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = 44000,
            visDates = "RSV : 2023-07-24",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Varivax"
        )
    )

    val pcv21 = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 1,
        name = "TestPcv21",
        productId = 410,
        salesProductId = 422,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 9999999, 18250, 410, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "PCV 21",
            categoryId = ProductCategory.VACCINE,
            description = "Capvaxive Prefilled Syringe(s) (0.5 mL)",
            displayName = "PCV 21 (Capvaxive)",
            id = 410,
            inventoryGroup = "PCV 21",
            lossFee = 24412,
            productNdc = "N/A",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = 24412,
            visDates = "Meningococcal: 2021-08-06",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Capvaxive"
        )
    )

    val pcv13 = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 25,
        name = "TestPcv13",
        productId = 25,
        salesProductId = 51,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 6569, 42, 25, null),
            AgeIndication(Gender.MF, 2, 1073741824, 18250, 25, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "PCV13",
            categoryId = ProductCategory.VACCINE,
            description = "Pneumococcal 13-valent Conjugate Vaccine (Pediatric)",
            displayName = "PCV13 (Prevnar 13)",
            id = 25,
            inventoryGroup = "PCV20",
            lossFee = 24412,
            productNdc = "N/A",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.PREFILLED_SYRINGE,
            purchaseOrderFee = 24412,
            visDates = "PCV13: 2019-10-30",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Prevnar"
        )
    )

    val fluDose = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 25,
        name = "TestFlu",
        productId = 394,
        salesProductId = 402,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.MF, 1, 18249, 730, 394, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "Influenza",
            categoryId = ProductCategory.VACCINE,
            description = "FluMist Tri, 10 Nasal Spray(s) (0.2 mL)  Ages 2-49",
            displayName = "FluMist Tri NS 24-25 (+24 Mos) NS",
            id = 394,
            inventoryGroup = "FluMist Tri (+24 Mos) NS",
            lossFee = 2500,
            productNdc = "N/A",
            routeCode = RouteCode.IM,
            presentation = ProductPresentation.NASAL_SPRAY,
            purchaseOrderFee = 2200,
            visDates = "Influenza: 2021-08-06",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "FluMist Tri"
        )
    )

    val larcDose = LotNumberWithProduct(
        expirationDate = LocalDate.now().plusDays(500),
        id = 25,
        name = "Kyleena",
        productId = 285,
        salesProductId = 286,
        salesLotNumberId = 1,
        ageIndications = listOf(
            AgeIndication(Gender.F, 1, 9999999, 3650, 285, null)
        ),
        cptCodes = listOf(),
        product = Product(
            antigen = "IUD",
            categoryId = ProductCategory.LARC,
            description = "Kyleena, 1 INTRAUTERINE DEVICE in 1 CARTON",
            displayName = "IUD (Kyleena, IUD)",
            id = 285,
            inventoryGroup = "FluMist Tri (+24 Mos) NS",
            lossFee = 2500,
            productNdc = "N/A",
            routeCode = RouteCode.IUD,
            presentation = ProductPresentation.IUD,
            purchaseOrderFee = 2200,
            visDates = "IUD: 2016-09-19",
            status = ProductStatus.VACCINE_ENABLED,
            prettyName = "Kyleena, IUD"
        )
    )
}
