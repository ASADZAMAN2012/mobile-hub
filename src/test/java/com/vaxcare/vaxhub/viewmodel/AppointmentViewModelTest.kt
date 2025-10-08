/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.data.MockProducts
import com.vaxcare.vaxhub.data.dao.AppointmentDao
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.domain.partd.ConvertPartDCopayToProductUseCase
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.repository.AppointmentRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppointmentViewModelTest {
    private val appointmentDao: AppointmentDao = mockk()
    private val localStorage: LocalStorage = mockk(relaxed = true)
    private val appointmentRepository: AppointmentRepository = mockk()
    private val appointmentMediaUseCase: UploadAppointmentMediaUseCase = mockk()
    private val convertCopayToProduct: ConvertPartDCopayToProductUseCase = mockk()
    private lateinit var viewModel: AppointmentViewModel

    @Before
    fun setup() {
        viewModel = AppointmentViewModel(
            appointmentDao,
            localStorage,
            appointmentRepository,
            appointmentMediaUseCase,
        )
        every { localStorage.tabletId } returns "deviceID"
    }

    @Test
    fun `Verify CurrentCheckout contains MedD Doses`() =
        run {
            viewModel.currentCheckout = CurrentCheckout(
                stagedProducts = mutableListOf(
                    VaccineAdapterProductDto(
                        lotNumberWithProduct = MockProducts.rsvDoseZeroCopay,
                        doseSeries = null,
                        paymentMode = PaymentMode.InsurancePay,
                        appointmentPaymentMethod = PaymentMethod.InsurancePay
                    )
                )
            )

            assertTrue(
                "Current Checkout contains MedD Doses",
                viewModel.currentCheckout.containsMedDDoses()
            )
        }

    @Test
    fun `Verify CurrentCheckout doesn't contain MedD Doses`() =
        run {
            viewModel.currentCheckout = CurrentCheckout(
                stagedProducts = mutableListOf(
                    VaccineAdapterProductDto(
                        lotNumberWithProduct = MockProducts.ipolDose,
                        doseSeries = null,
                        paymentMode = PaymentMode.InsurancePay,
                        appointmentPaymentMethod = PaymentMethod.InsurancePay
                    ),
                    VaccineAdapterProductDto(
                        lotNumberWithProduct = MockProducts.rsvDoseZeroCopay,
                        doseSeries = null,
                        paymentMode = PaymentMode.InsurancePay,
                        appointmentPaymentMethod = PaymentMethod.InsurancePay
                    ).also { it.doseState = DoseState.REMOVED }
                )
            )

            assertFalse(
                "Current Checkout contains MedD Doses",
                viewModel.currentCheckout.containsMedDDoses()
            )
        }
}
