/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.data.TestAppointments
import com.vaxcare.vaxhub.data.TestProductWithIssues
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import org.junit.Test

class VaccineIssuesTest {
    private val verifier = ProductVerifier(
        HubFeatures(
            isRprdAndNotLocallyCreated = true,
            isDisableDuplicateRSV = false
        )
    )

    @Test
    fun testOutOfAge() =
        baseVaccineIssuesTest(
            product = TestProductWithIssues.OutOfAgeIndicated,
            assertIssue = ProductIssue.OutOfAgeIndication
        )

    @Test
    fun testExpiredProduct() {
        baseVaccineIssuesTest(
            product = TestProductWithIssues.OutOfAgeIndicated,
            assertIssue = ProductIssue.Expired
        )
    }

    @Test
    fun testRequireCopay() =
        baseVaccineIssuesTest(
            product = TestProductWithIssues.MedDProduct,
            assertIssue = ProductIssue.CopayRequired
        )

    @Test
    fun testRejectCodeNotCovered() =
        baseVaccineIssuesTest(
            product = TestProductWithIssues.ExpiredProduct,
            assertIssue = ProductIssue.ProductNotCovered
        )

    private fun baseVaccineIssuesTest(product: LotNumberWithProduct, assertIssue: ProductIssue) {
        val appointment = TestAppointments.medDAppt

        val vaccine = verifier.getProductIssues(
            productToCheck = product,
            appointment = appointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )

        assert(vaccine != null)
        assert(vaccine!!.issues.contains(assertIssue))
    }
}
