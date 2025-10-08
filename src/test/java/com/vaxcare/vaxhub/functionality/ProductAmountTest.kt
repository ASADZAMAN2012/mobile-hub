/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.vaxhub.data.MockAppointments
import com.vaxcare.vaxhub.data.MockProducts
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import org.junit.Test

class ProductAmountTest {
    private val features = HubFeatures(
        isRprdAndNotLocallyCreated = false,
        isDisableDuplicateRSV = false
    )
    private val validator = ProductVerifier(features)

    @Test
    fun `Validate SelfPay Amount Over Zero`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckPostRun
        val mockPediatricDose = MockProducts.pediatricTdapDose
        val result = validator.getProductIssues(
            productToCheck = mockPediatricDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )
            ?.toAdapterDto()
            ?.apply { paymentMode = PaymentMode.SelfPay }

        assert(result?.isSelfPayAndNonZeroRate() == true)
    }

    @Test
    fun `Validate SelfPay Amount Is Zero`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckNotRun
        val mockRsvDose = MockProducts.pediatricTdapDoseWithZeroSelfPayRate
        val result = validator.getProductIssues(
            productToCheck = mockRsvDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )
            ?.toAdapterDto()
            ?.apply { paymentMode = PaymentMode.SelfPay }

        assert(result?.isSelfPayAndNonZeroRate() == false)
    }
}
