/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.ui.checkout.CheckoutCompleteFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.PaymentSubmitFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogArgs
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDCopayDialogDirections
import com.vaxcare.vaxhub.ui.checkout.dialog.SelfPayDialogDirections
import com.vaxcare.vaxhub.ui.checkout.summary.CheckoutSummaryFragmentDirections

interface CheckoutSummaryDestination {
    fun goBack(fragment: Fragment?, data: Map<String, Any>? = null)

    fun toCheckoutComplete(
        fragment: Fragment?,
        shotCount: Int,
        multiplePaymentMode: Boolean
    )

    fun toCheckoutCompleteFromSignature(
        fragment: Fragment?,
        shotCount: Int,
        multiplePaymentMode: Boolean
    )

    fun toMedDSummary(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    )

    fun toMedDSignatureCollect(fragment: Fragment?)

    fun toMedDCopayDialog(
        fragment: Fragment?,
        appointmentId: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean,
    )

    fun toSelfPayDialog(
        fragment: Fragment?,
        appointmentId: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean,
    )

    fun goToConfirmParentInfo(fragment: Fragment?, appointmentId: Int)

    fun goToCollectPaymentFromSelfPay(
        fragment: Fragment?,
        ordinalFlow: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean
    )

    fun goToMedDSummaryFromSelfPay(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInformation: PaymentInformationRequestBody?
    )

    fun goToCollectPaymentFromMedDCopay(
        fragment: Fragment?,
        ordinalFlow: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean
    )

    fun goToMedDSummaryFromMedDCopay(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInformation: PaymentInformationRequestBody?
    )

    fun goToErrorDialog(fragment: Fragment?, args: ErrorDialogArgs)

    fun goToPaymentSummaryFromPaymentSubmit(fragment: Fragment?, data: Map<String, String?>)
}

class CheckoutSummaryDestinationImpl(private val navCommons: NavCommons) :
    CheckoutSummaryDestination {
    override fun goBack(fragment: Fragment?, data: Map<String, Any>?) = navCommons.goBack(fragment, data)

    override fun toCheckoutComplete(
        fragment: Fragment?,
        shotCount: Int,
        multiplePaymentMode: Boolean
    ) {
        val action =
            CheckoutSummaryFragmentDirections.actionCheckoutSummaryFragmentToCheckoutComplete(
                shotCount = shotCount,
                isMultiplePayment = multiplePaymentMode
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toCheckoutCompleteFromSignature(
        fragment: Fragment?,
        shotCount: Int,
        multiplePaymentMode: Boolean
    ) {
        val action =
            PaymentSubmitFragmentDirections.actionPaymentSubmitFragmentToCheckoutComplete(
                shotCount = shotCount,
                isMultiplePayment = multiplePaymentMode
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun toMedDSummary(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInfo: PaymentInformationRequestBody?
    ) {
        val action =
            CheckoutSummaryFragmentDirections.actionCheckoutSummaryFragmentToPaymentSummaryFragment(
                appointmentId = appointmentId,
                paymentInformation = paymentInfo
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toMedDSignatureCollect(fragment: Fragment?) {
        val action = CheckoutSummaryFragmentDirections
            .actionCheckoutSummaryFragmentToMedDSignatureCollectFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun toMedDCopayDialog(
        fragment: Fragment?,
        appointmentId: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean,
    ) {
        val action =
            CheckoutSummaryFragmentDirections.actionCheckoutSummaryFragmentToMedDCopayDialog(
                appointmentId = appointmentId,
                currentPhone = currentPhone,
                enablePhoneCollection = enablePhoneCollection
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun toSelfPayDialog(
        fragment: Fragment?,
        appointmentId: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean,
    ) {
        val action = CheckoutSummaryFragmentDirections.actionCheckoutSummaryFragmentToSelfPayDialog(
            appointmentId = appointmentId,
            currentPhone = currentPhone,
            enablePhoneCollection = enablePhoneCollection
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToConfirmParentInfo(fragment: Fragment?, appointmentId: Int) {
        val action =
            CheckoutCompleteFragmentDirections.actionCheckoutCompleteToConfirmParentInfoFragment(
                appointmentId = appointmentId
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToCollectPaymentFromSelfPay(
        fragment: Fragment?,
        ordinalFlow: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean
    ) {
        val action = SelfPayDialogDirections.actionSelfPayDialogToCollectPaymentFragment(
            ordinalFlow = ordinalFlow,
            currentPhone = currentPhone,
            enablePhoneCollection = enablePhoneCollection
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToMedDSummaryFromSelfPay(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInformation: PaymentInformationRequestBody?
    ) {
        val action = SelfPayDialogDirections.actionSelfPayDialogToPaymentSummaryFragment(
            appointmentId = appointmentId,
            paymentInformation = paymentInformation
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCollectPaymentFromMedDCopay(
        fragment: Fragment?,
        ordinalFlow: Int,
        currentPhone: String?,
        enablePhoneCollection: Boolean
    ) {
        val action = MedDCopayDialogDirections.actionMedDCopayDialogToCollectPaymentFragment(
            ordinalFlow = ordinalFlow,
            currentPhone = currentPhone,
            enablePhoneCollection = enablePhoneCollection
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToMedDSummaryFromMedDCopay(
        fragment: Fragment?,
        appointmentId: Int,
        paymentInformation: PaymentInformationRequestBody?
    ) {
        val action = MedDCopayDialogDirections.actionMedDCopayDialogToPaymentSummaryFragment(
            appointmentId = appointmentId,
            paymentInformation = paymentInformation
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToErrorDialog(fragment: Fragment?, args: ErrorDialogArgs) {
        val action = PaymentSubmitFragmentDirections.actionGlobalErrorDialog(
            title = args.title,
            body = args.body,
            primaryButton = args.primaryButton,
            secondaryButton = args.secondaryButton,
            optionalArg = args.optionalArg
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToPaymentSummaryFromPaymentSubmit(fragment: Fragment?, data: Map<String, String?>) {
        navCommons.goBackPopTo(
            fragment = fragment,
            destinationId = R.id.paymentSummaryFragment,
            backData = data
        )
    }
}
