/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.functionality

import android.content.Context
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.data.MockProducts
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.extension.amount
import com.vaxcare.vaxhub.model.extension.getIssues
import com.vaxcare.vaxhub.model.extension.isMultiplePaymentMode
import io.mockk.every
import io.mockk.mockk
import org.junit.Ignore
import org.junit.Test

class VaccineAdapterModelExtTest {
    private val context = mockk<Context>()

    @Test
    fun `Copay amount test`() {
        val products = listOf(
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.rsvDoseZeroCopay,
                doseSeries = null,
                paymentMode = null,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.tdapDoseTenCopay,
                doseSeries = null,
                paymentMode = null,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.zosterDoseOneHundredSelfPay,
                doseSeries = null,
                paymentMode = null,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            )
        )

        /*
            (rsvDoseZeroCopay = 0.0) +
            (tdapDoseTenCopay = 10.0) +
            (zosterDoseOneHundredSelfPay = 1.0) == 11.0
         */
        val expectedTotal = 11.0

        val actualAmount = products.amount().toDouble()
        assert(expectedTotal == actualAmount)
    }

    @Test
    fun `SelfPay and Copay amount test`() {
        val products = listOf(
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.rsvDoseZeroCopay,
                doseSeries = null,
                paymentMode = PaymentMode.PartnerBill,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.tdapDoseTenCopay,
                doseSeries = null,
                paymentMode = PaymentMode.PartnerBill,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.zosterDoseOneHundredSelfPay,
                doseSeries = null,
                paymentMode = PaymentMode.SelfPay,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            )
        )

        /*
            (rsvDoseZeroCopay = 0.0) +
            (tdapDoseTenCopay = 10.0) +
            (zosterDoseOneHundredSelfPay = 100.0) == 110.0
         */
        val expectedTotal = 110.0

        val actualAmount = products.amount().toDouble()
        assert(expectedTotal == actualAmount)
    }

    @Test
    fun `Multiple PaymentMode test`() {
        val products = listOf(
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.rsvDoseZeroCopay,
                doseSeries = null,
                paymentMode = null,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.tdapDoseTenCopay,
                doseSeries = null,
                paymentMode = PaymentMode.PartnerBill,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            ),
            VaccineAdapterProductDto(
                lotNumberWithProduct = MockProducts.zosterDoseOneHundredSelfPay,
                doseSeries = null,
                paymentMode = PaymentMode.SelfPay,
                appointmentPaymentMethod = PaymentMethod.InsurancePay
            )
        )

        assert(products.isMultiplePaymentMode())
    }

    @Ignore("Ignored: context can be mocked sure but not SpannableStringBuilder inside of getIssues")
    @Test
    fun `Issues test`() {
        every { context.getString(R.string.scanned_dose_expired_header) } returns "Expired Dose"
        every { context.getString(R.string.wrong_stock) } returns "Wrong Stock"
        every { context.getString(R.string.scanned_dose_age_issue_header) } returns "Out of Age Indication"
        every { context.getString(R.string.scanned_dose_not_flu_header) } returns "Patient can only receive flu"
        val vaccine = VaccineWithIssues.Builder()
            .lotNumberWithProduct(MockProducts.zosterDoseOneHundredSelfPay)
            .issues(ProductIssue.Expired)
            .build()?.toAdapterDto()
        val issues = vaccine?.getIssues(context) // runtime exception
        assert(issues == "Expired Dose")
    }
}
