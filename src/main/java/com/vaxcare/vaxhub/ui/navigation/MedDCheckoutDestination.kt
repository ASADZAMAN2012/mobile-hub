/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode
import com.vaxcare.vaxhub.ui.checkout.CheckoutNurseDisclaimerFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.CollectPaymentFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.MedDSignatureCollectFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.summary.PaymentSummaryFragmentDirections

interface MedDCheckoutDestination {
    fun toChangePaymentMethodDialog(fragment: Fragment?, changePaymentMethodMode: ChangePaymentMethodMode)

    fun toSignatureCollectFragment(
        fragment: Fragment?,
        paymentInfo: PaymentInformationRequestBody?,
        existingSignatureUri: String?
    )

    fun toSignatureSubmitFragment(fragment: Fragment?, paymentInfo: PaymentInformationRequestBody?)

    fun goToPatientNoCard(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    )

    fun goToPaymentSummaryFromCollectPayment(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    )

    fun goBackPaymentCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        enablePhoneCollection: Boolean
    )

    fun goBackToProductGrid(fragment: Fragment?)

    fun goToSignatureSubmit(
        fragment: Fragment?,
        paymentInformation: PaymentInformationRequestBody?,
        fileUri: String?
    )

    fun goToNurseDisclaimer(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?,
        preAccepted: Boolean
    )

    fun goToPaymentSummaryFromDisclaimer(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    )
}

class MedDCheckoutDestinationImpl(private val navCommons: NavCommons) : MedDCheckoutDestination {
    override fun toChangePaymentMethodDialog(fragment: Fragment?, changePaymentMethodMode: ChangePaymentMethodMode) {
        val action =
            PaymentSummaryFragmentDirections.actionPaymentSummaryFragmentToChangePaymentMethodDialog(
                mode = changePaymentMethodMode
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toSignatureCollectFragment(
        fragment: Fragment?,
        paymentInfo: PaymentInformationRequestBody?,
        existingSignatureUri: String?
    ) {
        val action =
            PaymentSummaryFragmentDirections.actionPaymentSummaryFragmentToMedDSignatureCollectFragment(
                paymentInformation = paymentInfo,
                signatureUri = existingSignatureUri
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toSignatureSubmitFragment(fragment: Fragment?, paymentInfo: PaymentInformationRequestBody?) {
        val action =
            PaymentSummaryFragmentDirections.actionPaymentSummaryFragmentToPaymentSubmitFragment(
                paymentInformation = paymentInfo,
                selfpayCashOrCheck = paymentInfo == null
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPatientNoCard(
        fragment: Fragment?,
        flow: NoInsuranceCardFlow,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        val directions =
            CollectPaymentFragmentDirections.actionCollectPaymentFragmentToPatientNoCardFragment(
                flow = flow,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            )
        navCommons.goToFragment(fragment, directions)
    }

    override fun goToPaymentSummaryFromCollectPayment(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    ) {
        val directions =
            CollectPaymentFragmentDirections.actionCollectPaymentFragmentToPaymentSummaryFragment(
                appointmentId = appointmentId,
                paymentInformation = paymentInfo
            )
        navCommons.goToFragment(fragment, directions)
    }

    override fun goBackPaymentCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        enablePhoneCollection: Boolean
    ) {
        if (!navCommons.goBackPopTo(
                fragment = fragment,
                destinationId = R.id.collectPaymentFragment,
                inclusive = false
            )
        ) {
            val directions =
                PaymentSummaryFragmentDirections.actionPaymentSummaryFragmentToCollectPaymentFragment(
                    ordinalFlow = NoInsuranceCardFlow.CHECKOUT_PATIENT.ordinal,
                    enablePhoneCollection = enablePhoneCollection
                )
            navCommons.goToFragment(fragment, directions)
        }
    }

    override fun goBackToProductGrid(fragment: Fragment?) {
        navCommons.goBackPopTo(
            fragment = fragment,
            destinationId = R.id.checkoutPatientFragment,
            inclusive = false
        )
    }

    override fun goToSignatureSubmit(
        fragment: Fragment?,
        paymentInformation: PaymentInformationRequestBody?,
        fileUri: String?
    ) {
        val action =
            MedDSignatureCollectFragmentDirections.actionMedDSignatureCollectFragmentToPaymentSubmitFragment(
                paymentInformation = paymentInformation,
                fileUri = fileUri
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToNurseDisclaimer(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?,
        preAccepted: Boolean
    ) {
        val action =
            CollectPaymentFragmentDirections.actionCollectPaymentFragmentToCheckoutNurseDisclaimerFragment(
                preAccepted = preAccepted,
                appointmentId = appointmentId,
                paymentInformation = paymentInfo
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToPaymentSummaryFromDisclaimer(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    ) {
        val action =
            CheckoutNurseDisclaimerFragmentDirections.actionCheckoutNurseDisclaimerFragmentToPaymentSummaryFragment(
                appointmentId = appointmentId,
                paymentInformation = paymentInfo
            )

        navCommons.goToFragment(fragment, action)
    }
}
