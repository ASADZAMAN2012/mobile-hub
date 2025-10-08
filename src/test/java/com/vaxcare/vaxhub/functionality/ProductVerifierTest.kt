/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.data.MockAppointments
import com.vaxcare.vaxhub.data.MockProducts
import com.vaxcare.vaxhub.data.TestAppointments
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct
import com.vaxcare.vaxhub.model.inventory.validator.HubFeatures
import com.vaxcare.vaxhub.model.inventory.validator.ProductVerifier
import org.junit.Test
import java.math.BigDecimal

class ProductVerifierTest {
    private val features = HubFeatures(
        isRprdAndNotLocallyCreated = false,
        isDisableDuplicateRSV = false
    )
    private val validator = ProductVerifier(features)

    @Test
    fun `Validate out of Age dose`() {
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

        assert(result?.issues?.contains(ProductIssue.OutOfAgeIndication) == true)
    }

    @Test
    fun `Validate MedD Copay Issue - RSV`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckNotRun
        val mockRsvDose = MockProducts.rsvDoseMissingCopay
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

        assert(result?.issues?.contains(ProductIssue.CopayRequired) == true)
    }

    @Test
    fun `Validate Needs RouteSelection - IPOL`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckNotRun
        val mockIpolDose = MockProducts.ipolDose
        val result = validator.getProductIssues(
            productToCheck = mockIpolDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues

        assert(result?.contains(ProductIssue.RouteSelectionRequired) == true)
    }

    @Test
    fun `Validate Needs RouteSelection - Varivax`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckNotRun
        val mockvarivaxDose = MockProducts.varivaxDose
        val result = validator.getProductIssues(
            productToCheck = mockvarivaxDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues

        assert(result?.contains(ProductIssue.RouteSelectionRequired) == false)
    }

    @Test
    fun `Validate Duplicate Product - Varivax`() {
        val mockAppointment = TestAppointments.pediatricAppt
        val varivaxDose = MockProducts.varivaxDose

        val existingDose = VaccineAdapterProductDto(
            MockProducts.varivaxDose,
            null,
            PaymentMode.InsurancePay,
            PaymentMethod.InsurancePay
        )

        val result = validator.getProductIssues(
            productToCheck = varivaxDose,
            appointment = mockAppointment,
            stagedProducts = listOf(existingDose),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues

        assert(result?.contains(ProductIssue.DuplicateProductException) == false)
        assert(result?.contains(ProductIssue.DuplicateProduct) == true)
    }

    @Test
    fun `Validate Duplicate Product - Beyfortus`() {
        val mockAppointment = TestAppointments.pediatricAppt
        val beyfortusDose = MockProducts.beyfortus

        val existingDose = VaccineAdapterProductDto(
            MockProducts.beyfortus,
            null,
            PaymentMode.InsurancePay,
            PaymentMethod.InsurancePay
        )

        val result = validator.getProductIssues(
            productToCheck = beyfortusDose,
            appointment = mockAppointment,
            stagedProducts = listOf(existingDose),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues

        assert(result?.contains(ProductIssue.DuplicateProductException) == true)
    }

    @Test
    fun `Validate Wrong Stock`() {
        val mockAppointment = TestAppointments.medDAppt
        val ipol = MockProducts.ipolDose

        val result = validator.getProductIssues(
            productToCheck = ipol,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = listOf(
                SimpleOnHandProduct(
                    lotNumberName = ipol.name,
                    inventorySource = InventorySource.ANOTHER_STOCK,
                    onHandAmount = 1
                )
            ),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues?.sorted()

        assert(result?.first() == ProductIssue.WrongStock)
    }

    @Test
    fun `Validate MedB Inventory Group covered`() {
        val mockAppointment = TestAppointments.medDAppt

        val pcv21Result = validator.getProductIssues(
            productToCheck = MockProducts.pcv21,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = listOf(
                SimpleOnHandProduct(
                    lotNumberName = MockProducts.pcv21.name,
                    inventorySource = InventorySource.PRIVATE,
                    onHandAmount = 1
                )
            ),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues?.sorted()

        val pcv13Result = validator.getProductIssues(
            productToCheck = MockProducts.pcv13,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = listOf(
                SimpleOnHandProduct(
                    lotNumberName = MockProducts.pcv13.name,
                    inventorySource = InventorySource.PRIVATE,
                    onHandAmount = 1
                )
            ),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues?.sorted()

        assert(pcv21Result?.none { it == ProductIssue.ProductNotCovered } == true)
        assert(pcv13Result?.none { it == ProductIssue.ProductNotCovered } == true)
    }

    @Test
    fun `Validate Flu covered`() {
        val mockAppointment = TestAppointments.medDAppt

        val result = validator.getProductIssues(
            productToCheck = MockProducts.fluDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = listOf(
                SimpleOnHandProduct(
                    lotNumberName = MockProducts.fluDose.name,
                    inventorySource = InventorySource.PRIVATE,
                    onHandAmount = 1
                )
            ),
            isVaxCare3 = true,
            medDInfo = null
        )?.issues?.sorted()

        assert(result?.none { it == ProductIssue.ProductNotCovered } == true)
    }

    @Test
    fun `Validate MedD dose HealthSystems PaymentMode not overriden`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckFinishedSelfPay
        val mockRsvDose = MockProducts.rsvDoseMissingCopay
        val result = validator.getProductIssues(
            productToCheck = mockRsvDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = false,
            medDInfo = null
        )

        assert(result?.paymentMode == null)
    }

    @Test
    fun `Validate MedD dose HealthSystems PaymentMode is overridden`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckFinishedSelfPay
        val mockRsvDose = MockProducts.rsvDoseMissingCopay.apply {
            copay = ProductCopayInfo(
                antigen = MedDVaccines.RSV,
                copay = BigDecimal.ZERO,
            )
        }
        val result = validator.getProductIssues(
            productToCheck = mockRsvDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = MedDInfo(
                eligible = true,
                copays = listOf(
                    ProductCopayInfo(
                        antigen = MedDVaccines.RSV,
                        copay = BigDecimal.ZERO,
                    )
                )
            )
        )

        assert(result?.paymentMode == PaymentMode.InsurancePay)
    }

    @Test
    fun `Validate LARC product scanned`() {
        val mockAppointment = MockAppointments.medDAppointmentCheckFinishedSelfPay
        val larcDose = MockProducts.larcDose
        val result = validator.getProductIssues(
            productToCheck = larcDose,
            appointment = mockAppointment,
            stagedProducts = emptyList(),
            doseSeries = null,
            manualDob = null,
            simpleOnHandProductInventory = emptyList(),
            isVaxCare3 = true,
            medDInfo = null
        )

        assert(result?.issues?.contains(ProductIssue.LarcAdded) == true)
    }
}
