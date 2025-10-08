/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CheckoutTerminus
import com.vaxcare.vaxhub.model.MissingInfo
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.metric.MedDCheckRunMetric
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import com.vaxcare.vaxhub.ui.checkout.CheckoutPatientFragmentDirections
import com.vaxcare.vaxhub.ui.checkout.dialog.BaseOneTouchCheckoutDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogArgs
import com.vaxcare.vaxhub.ui.checkout.dialog.InvalidScanMessageType
import com.vaxcare.vaxhub.ui.checkout.dialog.MedDReviewCopayDialogDirections
import com.vaxcare.vaxhub.ui.patient.ConfirmPatientInfoMode
import com.vaxcare.vaxhub.ui.patient.CurbsideConfirmPatientInfoFragmentArgs
import com.vaxcare.vaxhub.ui.patient.edit.PatientEditHandToPatientFragmentArgs
import java.time.LocalDate

interface CheckoutPatientDestination {
    fun toBackCheckoutDialog(fragment: Fragment?)

    fun toConfirmPatientInfo(
        fragment: Fragment?,
        patientId: Int,
        infoMode: ConfirmPatientInfoMode
    )

    fun toCheckoutPromptRemoveDose(fragment: Fragment?)

    fun showPromoDialog(
        fragment: Fragment?,
        appointment: Appointment,
        resId: Int,
        otPayRate: Double?
    )

    fun toAgeWarningBooleanPromptDialog(
        fragment: Fragment?,
        title: String,
        message: String
    )

    fun toScannedDoseIssue(fragment: Fragment?, issue: ProductIssue)

    fun popBackToCurbsidePatientSearch(fragment: Fragment?): Boolean

    fun popBackToAppointmentList(fragment: Fragment?, appointmentDate: LocalDate? = null): Boolean

    fun toCheckoutSummary(fragment: Fragment?, appointmentId: Int)

    fun redirectToCheckoutSummary(fragment: Fragment?, appointmentId: Int)

    fun toSelectDoseSeries(fragment: Fragment?, product: VaccineAdapterProductDto)

    fun toUnorderedDosePrompt(fragment: Fragment?, args: ErrorDialogArgs)

    fun toDoseReasonFragment(fragment: Fragment?, destinations: List<CheckoutTerminus>)

    fun toNewPayerScreen(fragment: Fragment?, missingInfo: MissingInfo)

    fun toSelectPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper)

    fun toPatientInfoScreen(fragment: Fragment?, missingInfo: MissingInfo)

    fun goToMedDReviewCopay(
        fragment: Fragment?,
        appointmentId: Int,
        antigen: String,
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean
    )

    fun goToCopayCheckFromMedDReview(
        medDReviewCopayDialog: Fragment,
        appointmentId: Int,
        duringCheckout: MedDCheckRunMetric.CheckContext,
        copayAntigen: String,
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean,
        medDCheckStartedAt: String? = null
    )

    fun goToMedDCheckFromCheckout(
        fragment: Fragment,
        appointmentId: Int,
        duringCheckout: MedDCheckRunMetric.CheckContext,
        antigen: String = "None",
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean,
        medDCheckStartedAt: String? = null
    )

    fun goToNotInCovidAssistDialog(fragment: Fragment, behaviorOrdinal: Int)

    fun goToRouteRequiredDialog(
        fragment: Fragment,
        productId: Int,
        productName: String,
        lotNumber: String,
        appointmentId: Int
    )

    fun goToWrongStockDialog(
        fragment: Fragment,
        appointmentInventorySource: InventorySource,
        isStockSelectorEnabled: Boolean
    )

    fun goToDuplicateRSVExceptionDialog(
        fragment: Fragment,
        shouldShowPaymentFlip: Boolean,
        otPayRate: String?
    )

    fun goToInvalidScanPrompt(
        fragment: Fragment?,
        title: String,
        messageToShow: String,
        messageType: InvalidScanMessageType
    )

    fun goToScanInsuranceScreen(fragment: Fragment?)

    fun goToInsurancePhoneCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    )

    fun goToRefreshOrdersDialog(fragment: Fragment)
}

class CheckoutPatientDestinationImpl(private val navCommons: NavCommons) :
    CheckoutPatientDestination {
    override fun toBackCheckoutDialog(fragment: Fragment?) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToBackCheckoutDialog()
        navCommons.goToFragment(fragment, action)
    }

    override fun toConfirmPatientInfo(
        fragment: Fragment?,
        patientId: Int,
        infoMode: ConfirmPatientInfoMode
    ) {
        navCommons.goToFragment(
            fragment,
            R.id.curbsideConfirmPatientInfoFragment,
            CurbsideConfirmPatientInfoFragmentArgs(patientId, infoMode).toBundle()
        )
    }

    override fun popBackToCurbsidePatientSearch(fragment: Fragment?): Boolean =
        navCommons.goBackPopTo(fragment, R.id.curbsidePatientSearchFragment)

    override fun popBackToAppointmentList(fragment: Fragment?, appointmentDate: LocalDate?): Boolean {
        return navCommons.goBackPopTo(fragment, R.id.appointmentListFragment)
    }

    override fun toCheckoutSummary(fragment: Fragment?, appointmentId: Int) {
        val action =
            CheckoutPatientFragmentDirections.actionGlobalCheckoutSummaryFragment(appointmentId)
        navCommons.goToFragment(fragment, action)
    }

    override fun redirectToCheckoutSummary(fragment: Fragment?, appointmentId: Int) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToSummaryFragment(
                appointmentId
            )
        navCommons.goToFragmentPopTo(fragment, action, false, R.id.appointmentListFragment)
    }

    override fun toSelectDoseSeries(fragment: Fragment?, product: VaccineAdapterProductDto) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToSelectDoseSeriesDialog(
                selectedVaccineId = product.id,
                dosesInSeries = product.dosesInSeries,
                doseSeries = product.doseSeries ?: 0
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toCheckoutPromptRemoveDose(fragment: Fragment?) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToCheckoutPromptRemoveDoseDialog()
        navCommons.goToFragment(fragment, action)
    }

    override fun showPromoDialog(
        fragment: Fragment?,
        appointment: Appointment,
        resId: Int,
        otPayRate: Double?
    ) {
        val bundle = bundleOf(
            BaseOneTouchCheckoutDialog.INSURANCE_NAME to appointment.paymentType,
            BaseOneTouchCheckoutDialog.ONE_TOUCH to otPayRate
        )
        navCommons.goToFragment(fragment, resId, bundle)
    }

    override fun toAgeWarningBooleanPromptDialog(
        fragment: Fragment?,
        title: String,
        message: String
    ) {
        val action = CheckoutPatientFragmentDirections
            .actionCheckoutVaccineFragmentToCovidHighRiskAttestationDialog(title, message)
        navCommons.goToFragment(fragment, action)
    }

    override fun toScannedDoseIssue(fragment: Fragment?, issue: ProductIssue) {
        val action =
            CheckoutPatientFragmentDirections
                .actionCheckoutVaccineFragmentToScannedDoseIssueDialog(
                    productIssue = issue
                )
        navCommons.goToFragment(fragment, action)
    }

    override fun toUnorderedDosePrompt(fragment: Fragment?, args: ErrorDialogArgs) {
        val action = CheckoutPatientFragmentDirections.actionGlobalErrorDialog(
            args.title,
            args.body,
            args.primaryButton,
            args.secondaryButton
        )
        navCommons.goToFragment(fragment, action)
    }

    override fun toDoseReasonFragment(fragment: Fragment?, destinations: List<CheckoutTerminus>) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToDoseReasonFragment(
                infoWrapper = destinations.filterIsInstance<MissingInfo>()
                    .firstOrNull()?.invalidInfoWrapper,
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun toNewPayerScreen(fragment: Fragment?, missingInfo: MissingInfo) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToCollectPayerInfoFragment(
                infoWrapper = missingInfo.invalidInfoWrapper
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toSelectPayerScreen(fragment: Fragment?, infoWrapper: InvalidInfoWrapper) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToCollectPayerFragment(
                infoWrapper
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun toPatientInfoScreen(fragment: Fragment?, missingInfo: MissingInfo) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToCollectDemoInfoFragment(
                infoWrapper = missingInfo.invalidInfoWrapper
            )
        navCommons.goToFragment(fragment, action)
    }

    override fun goToMedDReviewCopay(
        fragment: Fragment?,
        appointmentId: Int,
        antigen: String,
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean
    ) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToMedDReviewCopayDialog(
                appointmentId = appointmentId,
                copayAntigen = antigen,
                isCheckMbi = isCheckMbi,
                isMedDCheckStartedAlready = isMedDCheckStartedAlready
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToCopayCheckFromMedDReview(
        medDReviewCopayDialog: Fragment,
        appointmentId: Int,
        duringCheckout: MedDCheckRunMetric.CheckContext,
        copayAntigen: String,
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean,
        medDCheckStartedAt: String?
    ) {
        val action = MedDReviewCopayDialogDirections.actionMedDReviewCopayDialogToMedDCheckFragment(
            appointmentId = appointmentId,
            checkoutContext = duringCheckout.ordinal,
            copayAntigen = copayAntigen,
            isCheckMbi = isCheckMbi,
            isMedDCheckStartedAlready = isMedDCheckStartedAlready,
            medDCheckStartedAt = medDCheckStartedAt
        )

        navCommons.goToFragment(medDReviewCopayDialog, action)
    }

    override fun goToMedDCheckFromCheckout(
        fragment: Fragment,
        appointmentId: Int,
        duringCheckout: MedDCheckRunMetric.CheckContext,
        antigen: String,
        isCheckMbi: Boolean,
        isMedDCheckStartedAlready: Boolean,
        medDCheckStartedAt: String?
    ) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToMedDCheckFragment(
                appointmentId = appointmentId,
                checkoutContext = duringCheckout.ordinal,
                copayAntigen = antigen,
                isCheckMbi = isCheckMbi,
                isMedDCheckStartedAlready = isMedDCheckStartedAlready,
                medDCheckStartedAt = medDCheckStartedAt
            )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToNotInCovidAssistDialog(fragment: Fragment, behaviorOrdinal: Int) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToNotInCOVIDAssistDialog(
                checkCovidAction = behaviorOrdinal
            )

        navCommons.goToFragment(
            fragment = fragment,
            action = action
        )
    }

    override fun goToRouteRequiredDialog(
        fragment: Fragment,
        productId: Int,
        productName: String,
        lotNumber: String,
        appointmentId: Int
    ) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToRouteRequiredDialog(
                productId = productId,
                productName = productName,
                appointmentId = appointmentId,
                lotNumber = lotNumber
            )

        navCommons.goToFragment(
            fragment = fragment,
            action = action
        )
    }

    override fun goToWrongStockDialog(
        fragment: Fragment,
        appointmentInventorySource: InventorySource,
        isStockSelectorEnabled: Boolean
    ) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToWrongStockDialog(
                appointmentStockType = appointmentInventorySource.displayName,
                isStockSelector = isStockSelectorEnabled
            )

        navCommons.goToFragment(fragment = fragment, action = action)
    }

    override fun goToDuplicateRSVExceptionDialog(
        fragment: Fragment,
        shouldShowPaymentFlip: Boolean,
        otPayRate: String?
    ) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToDuplicateRSVExceptionDialog(
                shouldShowPaymentFlip = shouldShowPaymentFlip,
                otPayRate = otPayRate
            )
        navCommons.goToFragment(fragment = fragment, action = action)
    }

    override fun goToInvalidScanPrompt(
        fragment: Fragment?,
        title: String,
        messageToShow: String,
        messageType: InvalidScanMessageType
    ) {
        val action = CheckoutPatientFragmentDirections.actionGlobalInvalidScan(
            displayMessage = messageToShow,
            title = title,
            messageType = messageType
        )

        navCommons.goToFragment(fragment, action)
    }

    override fun goToScanInsuranceScreen(fragment: Fragment?) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutVaccineFragmentToScanInsuranceFragment()
        navCommons.goToFragment(fragment, action)
    }

    override fun goToInsurancePhoneCollection(
        fragment: Fragment?,
        appointmentId: Int,
        patientId: Int,
        currentPhone: String?
    ) {
        navCommons.goToFragment(
            fragment,
            R.id.patientHandToPatientFragment,
            PatientEditHandToPatientFragmentArgs(
                flow = NoInsuranceCardFlow.CHECKOUT_PATIENT,
                appointmentId = appointmentId,
                patientId = patientId,
                currentPhone = currentPhone
            ).toBundle()
        )
    }

    override fun goToRefreshOrdersDialog(fragment: Fragment) {
        val action =
            CheckoutPatientFragmentDirections.actionCheckoutPatientFragmentToRefreshOrdersDialog()
        navCommons.goToFragment(fragment, action)
    }
}
