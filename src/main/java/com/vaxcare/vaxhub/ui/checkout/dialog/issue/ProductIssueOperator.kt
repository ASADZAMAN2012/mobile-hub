/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog.issue

import android.os.Bundle
import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutPromptResult
import com.vaxcare.core.report.model.checkout.CopayCheckUnavailableValidationMetric
import com.vaxcare.core.report.model.checkout.DoseNotCoveredValidationMetric
import com.vaxcare.core.report.model.checkout.OutOfAgeIndicationValidationMetric
import com.vaxcare.vaxhub.core.extension.getEnum
import com.vaxcare.vaxhub.core.extension.retrieveParcelable
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.CheckCopayAntigen
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.model.enums.PregnancyDurationOptions
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.enums.VaccineMarkCondition
import com.vaxcare.vaxhub.model.inventory.validator.DuplicateProductExceptionRules.Companion.DUPLICATE_RSV_PRODUCT_ID
import com.vaxcare.vaxhub.model.metric.DuplicateRSVMetric
import com.vaxcare.vaxhub.model.metric.OutOfAgeWarningMetric
import com.vaxcare.vaxhub.model.metric.UncoveredDosePromptPresentedMetric
import com.vaxcare.vaxhub.model.metric.WrongStockDialogMetric
import com.vaxcare.vaxhub.ui.checkout.CheckoutPatientFragment.Companion.WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_BUTTON
import com.vaxcare.vaxhub.ui.checkout.CheckoutPatientFragment.Companion.WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_KEY
import com.vaxcare.vaxhub.ui.checkout.MedDCheckFragment
import com.vaxcare.vaxhub.ui.checkout.dialog.AgeWarningBooleanPromptDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.BaseOneTouchCheckoutDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutOutOfAgeExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutPromptExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.CheckoutPromptMedDExclusionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.DuplicateRSVExceptionDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.DuplicateRSVExceptionDialog.Companion.DUPLICATE_RSV_USER_ACTION_KEY
import com.vaxcare.vaxhub.ui.checkout.dialog.RouteRequiredDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ScannedDoseIssueDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.KEEP_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.REMOVE_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.SET_STOCK
import timber.log.Timber

/**
 * Handles the mutation of the product based on incoming user input from dialog issues
 *
 * @property appointment associated appointment
 * @property vaccineWithIssues pending product to mutate
 * @property analytics for sending analytics when criteria is met
 */
class ProductIssueOperator(
    private val appointment: Appointment,
    private val stagedProducts: List<VaccineAdapterDto>,
    private val vaccineWithIssues: VaccineWithIssues,
    private val locationSources: List<InventorySource>,
    @MHAnalyticReport private val analytics: AnalyticReport
) {
    companion object {
        const val ADD_ISSUE_TOKEN = "addIssue"
        const val MEDD_INFO_UPDATED_TOKEN = "meddUpdated"
    }

    /**
     * Processes the response from the user coming from the result bundle and returns a
     * ProductPendingUserAction enum
     *
     * @param action token / key of the result
     * @param result bundle of data from the user
     * @param issueMachine DialogIssueMachine to apply the operation to if necessary
     * @return ProductPendingUserAction enum that denotes what further actions are requested from
     * the user in order to resolve the associated issue
     */
    fun processDialogResponseAndGetPendingAction(
        action: String,
        result: Bundle,
        issueMachine: DialogIssueMachine
    ): ProductPendingUserAction =
        when (action) {
            DuplicateRSVExceptionDialog.DUPLICATE_RSV_EXCEPTION_RESULT_KEY ->
                handleDuplicateRSVDialog(result, stagedProducts, issueMachine)

            MedDCheckFragment.MEDD_COPAY_CHECK_FRAGMENT_RESULT_KEY ->
                handleMedDCopayCheck(result, issueMachine)

            AgeWarningBooleanPromptDialog.DIALOG_RESULT_KEY -> {
                handleCovidHighRiskAttestationResult(result, issueMachine)
            }

            WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_KEY -> {
                handleWeeksPregnantResult(result, issueMachine)
            }

            CheckoutOutOfAgeExclusionDialog.OUT_OF_AGE_EXCLUSION_DIALOG_RESULT_KEY,
            CheckoutPromptExclusionDialog.CHECKOUT_EXCLUSION_DIALOG_RESULT_KEY,
            CheckoutPromptMedDExclusionDialog.MEDD_EXCLUSION_DIALOG_RESULT_KEY ->
                handlePaymentFlip(result, action, issueMachine)

            ScannedDoseIssueDialog.SCANNED_PRODUCT_ISSUE_DIALOG_RESULT_KEY ->
                handleMiscIssues(result, issueMachine)

            RouteRequiredDialog.ROUTE_REQUIRED_DIALOG_FRAGMENT_RESULT_KEY ->
                handleRouteSelection(result)

            WrongStockDialog.WRONG_STOCK_DIALOG_RESULT_KEY ->
                handleWrongStockSelection(result, issueMachine)

            ADD_ISSUE_TOKEN -> addIssueFromResult(result, issueMachine)

            else -> {
                Timber.i("Issue with parsing action for ProductIssueOperator: $action")
                ProductPendingUserAction.NONE
            }
        }

    /**
     * Adds the issue from the incoming bundle to the DialogIssueMachine
     */
    private fun addIssueFromResult(result: Bundle, issueMachine: DialogIssueMachine): ProductPendingUserAction {
        val issueToAdd: ProductIssue? = result.retrieveParcelable(ADD_ISSUE_TOKEN)
        issueToAdd?.let { issueMachine.forceAddIssue(it) }
        return ProductPendingUserAction.NONE
    }

    private fun handleDuplicateRSVDialog(
        result: Bundle,
        stagedProducts: List<VaccineAdapterDto>,
        issueMachine: DialogIssueMachine
    ): ProductPendingUserAction {
        val userSelection = result.getString(DUPLICATE_RSV_USER_ACTION_KEY)
        analytics.saveMetric(
            DuplicateRSVMetric(appointment.id, userSelection ?: "Unknown")
        )
        when (userSelection) {
            DuplicateRSVExceptionDialog.PARTNER_BILL,
            DuplicateRSVExceptionDialog.SELF_PAY -> {
                handleDuplicateRSVPaymentFlip(userSelection, stagedProducts, issueMachine)
            }

            DuplicateRSVExceptionDialog.KEEP_DOSE -> {
                handleDuplicateRSVKeepDose(issueMachine)
            }

            DuplicateRSVExceptionDialog.REMOVE_DOSE -> {
                issueMachine.cancelPendingIssues()
            }
        }

        return ProductPendingUserAction.NONE
    }

    private fun handleDuplicateRSVPaymentFlip(
        userSelection: String,
        stagedProducts: List<VaccineAdapterDto>,
        issueMachine: DialogIssueMachine
    ): ProductPendingUserAction {
        val newPayerMode =
            if (userSelection == DuplicateRSVExceptionDialog.PARTNER_BILL) {
                PaymentMode.PartnerBill
            } else {
                PaymentMode.SelfPay
            }

        issueMachine.forceRemoveIssues(ProductIssue.DuplicateLot, ProductIssue.DuplicateProduct)

        vaccineWithIssues.savePaymentModeRevertValuesBeforeDuplicating()
        vaccineWithIssues.orderNumber =
            findOrderNumberFromDuplicatedDose(stagedProducts, DUPLICATE_RSV_PRODUCT_ID)
        vaccineWithIssues.overridePaymentMode(
            newPaymentMode = newPayerMode,
            newPaymentModeReason = PaymentModeReason.ImmunizationsNotCovered,
            newVaccineMarkCondition = VaccineMarkCondition.NOT_COVERED
        )

        // If dose is kept after processing the rest of the issues, perform the pending action
        issueMachine.addActionAfterIssuesResolved {
            stagedProducts.filter { it.product.id == DUPLICATE_RSV_PRODUCT_ID }
                .forEach {
                    if (it is VaccineAdapterProductDto) {
                        it.savePaymentModeRevertValuesBeforeDuplicating()
                        it.originalPaymentMode = newPayerMode
                        it.paymentMode = newPayerMode
                        it.paymentModeReason = PaymentModeReason.ImmunizationsNotCovered
                        it.vaccineMarkCondition = VaccineMarkCondition.NOT_COVERED
                    }
                }
        }

        return ProductPendingUserAction.NONE
    }

    private fun handleDuplicateRSVKeepDose(issueMachine: DialogIssueMachine): ProductPendingUserAction {
        vaccineWithIssues.orderNumber =
            findOrderNumberFromDuplicatedDose(stagedProducts, DUPLICATE_RSV_PRODUCT_ID)
        issueMachine.forceRemoveIssues(ProductIssue.DuplicateLot)
        issueMachine.forceRemoveIssues(ProductIssue.DuplicateProduct)
        return ProductPendingUserAction.NONE
    }

    private fun findOrderNumberFromDuplicatedDose(stagedProducts: List<VaccineAdapterDto>, productId: Int): String? {
        var copyOrderNumber: String? = null
        stagedProducts.filter { it.product.id == productId }
            .forEach {
                if (it is VaccineAdapterProductDto) {
                    copyOrderNumber = it.orderNumber
                }
            }
        return copyOrderNumber
    }

    private fun handleMedDCopayCheck(result: Bundle, issueMachine: DialogIssueMachine): ProductPendingUserAction {
        val copayResult = result.getEnum(
            key = MedDCheckFragment.MATCH_COPAY_ANTIGEN,
            default = CheckCopayAntigen.DEFAULT
        )

        val copayInfo: ProductCopayInfo? =
            result.retrieveParcelable(MedDCheckFragment.MEDD_COPAY_CHECK_FRAGMENT_COPAY_KEY)

        when {
            copayResult != CheckCopayAntigen.COPAY_MATCHED ->
                issueMachine.forceAddIssue(ProductIssue.ProductNotCovered)
            else -> removeProductNotCoveredAndApplyCopay(issueMachine, copayInfo)
        }

        return ProductPendingUserAction.NONE
    }

    private fun removeProductNotCoveredAndApplyCopay(
        issueMachine: DialogIssueMachine,
        incomingCopay: ProductCopayInfo?
    ) {
        issueMachine.forceRemoveIssues(ProductIssue.ProductNotCovered)
        vaccineWithIssues.issues.remove(ProductIssue.ProductNotCovered)
        vaccineWithIssues.copay = incomingCopay
    }

    private fun handleCovidHighRiskAttestationResult(
        result: Bundle,
        issueMachine: DialogIssueMachine
    ): ProductPendingUserAction {
        val dialogAction = result.getEnum(
            key = AgeWarningBooleanPromptDialog.DIALOG_BUNDLE_KEY,
            default = DialogAction.CANCEL
        )

        if (dialogAction == DialogAction.POSITIVE) {
            issueMachine.forceRemoveIssues(ProductIssue.OutOfAgeIndication)
            vaccineWithIssues.issues.remove(ProductIssue.OutOfAgeIndication)
        } else {
            issueMachine.forceAddIssue(ProductIssue.OutOfAgeIndication)
            vaccineWithIssues.issues.add(ProductIssue.OutOfAgeIndication)
        }

        return ProductPendingUserAction.NONE
    }

    private fun handleWeeksPregnantResult(result: Bundle, issueMachine: DialogIssueMachine): ProductPendingUserAction {
        val weeksPregnant = result.getEnum(
            key = WEEKS_PREGNANT_AGE_WARNING_DIALOG_RESULT_BUTTON,
            default = PregnancyDurationOptions.DOES_NOT_QUALIFY
        )

        when (weeksPregnant) {
            PregnancyDurationOptions.DOES_NOT_QUALIFY -> {
                issueMachine.forceAddIssue(ProductIssue.OutOfAgeIndication)
                vaccineWithIssues.issues.add(ProductIssue.OutOfAgeIndication)
            }
            // all other options indicate patient qualifies
            else -> {
                issueMachine.forceRemoveIssues(ProductIssue.OutOfAgeIndication)
                vaccineWithIssues.issues.remove(ProductIssue.OutOfAgeIndication)
            }
        }

        analytics.saveMetric(
            OutOfAgeWarningMetric(
                appointment.id,
                vaccineWithIssues.lotNumberWithProduct.product.displayName,
                weeksPregnant
            )
        )

        return ProductPendingUserAction.NONE
    }

    private fun handlePaymentFlip(
        result: Bundle,
        action: String,
        issueMachine: DialogIssueMachine
    ): ProductPendingUserAction {
        val dialogResult =
            result.getEnum(
                key = BaseOneTouchCheckoutDialog.ONE_TOUCH_BUNDLE_RESULT_KEY,
                default = DialogAction.CANCEL
            )
        handlePaymentFlipDialogResult(
            dialogResult = dialogResult,
            action = action,
            appointment = appointment,
            vaccineWithIssues = vaccineWithIssues,
            dialogIssueMachine = issueMachine
        )

        return ProductPendingUserAction.NONE
    }

    private fun handleMiscIssues(result: Bundle, issueMachine: DialogIssueMachine): ProductPendingUserAction {
        val buttonClicked = result.getEnum(
            key = ScannedDoseIssueDialog.SCANNED_PRODUCT_ISSUE_DIALOG_BUNDLE_KEY,
            default = DialogAction.CANCEL
        )
        if (buttonClicked == DialogAction.CANCEL) {
            issueMachine.cancelPendingIssues()
        }

        return ProductPendingUserAction.NONE
    }

    private fun handleRouteSelection(result: Bundle): ProductPendingUserAction {
        val chosenRoute =
            result.getEnum(
                key = RouteRequiredDialog.ROUTE_SELECTED_BUNDLE_KEY,
                default = RouteCode.IM
            )
        vaccineWithIssues.lotNumberWithProduct.product.routeCode = chosenRoute
        return ProductPendingUserAction.NONE
    }

    private fun handleWrongStockSelection(result: Bundle, issueMachine: DialogIssueMachine): ProductPendingUserAction {
        val buttonClicked = result.getEnum(
            key = WrongStockDialog.OPTION_SELECTED_BUNDLE_KEY,
            default = REMOVE_DOSE
        )
        analytics.saveMetric(
            WrongStockDialogMetric(
                optionSelected = WrongStockDialog.Options.getMetricString(buttonClicked),
                appointmentStock = appointment.vaccineSupply,
                lotName = vaccineWithIssues.lotNumberWithProduct.name,
                lotStocks = vaccineWithIssues.sources.map { it.displayName },
                patientInsurancePrimaryId = appointment.patient.paymentInformation?.primaryInsuranceId,
                patientInsurancePlanId = appointment.patient.paymentInformation?.primaryInsurancePlanId,
                activePublicStocks = locationSources.map { it.displayName },
                visitId = appointment.id
            )
        )

        return when (buttonClicked) {
            KEEP_DOSE -> ProductPendingUserAction.NONE
            REMOVE_DOSE -> {
                issueMachine.cancelPendingIssues()
                ProductPendingUserAction.NONE
            }

            SET_STOCK -> ProductPendingUserAction.SET_STOCK
        }
    }

    /**
     * Applies the markCondition and the payment mode/reason to the vaccine according to the result
     * from the dialog
     *
     * @param dialogResult resulting DialogAction from the dialog
     * @param action key from the fragmentResult
     * @param appointment associated appointment
     * @param vaccineWithIssues associated vaccine to change
     */
    private fun handlePaymentFlipDialogResult(
        dialogResult: DialogAction,
        action: String,
        appointment: Appointment,
        vaccineWithIssues: VaccineWithIssues,
        dialogIssueMachine: DialogIssueMachine
    ): ProductPendingUserAction {
        val isOutOfAge =
            action == CheckoutOutOfAgeExclusionDialog.OUT_OF_AGE_EXCLUSION_DIALOG_RESULT_KEY

        when (dialogResult) {
            DialogAction.POSITIVE -> {
                val (paymentReason, vaccineMarkCondition) = if (isOutOfAge) {
                    PaymentModeReason.OutOfAgeIndication to VaccineMarkCondition.OUT_OF_AGE
                } else {
                    PaymentModeReason.ImmunizationsNotCovered to VaccineMarkCondition.NOT_COVERED
                }

                VaccineDialogFlipResults(
                    promptResult = CheckoutPromptResult.PARTNER_BILL,
                    userSelection = UncoveredDosePromptPresentedMetric.UserSelection.PARTNER_BILL.displayName,
                    paymentModeReason = paymentReason,
                    vaccineMarkCondition = vaccineMarkCondition,
                    paymentMode = PaymentMode.PartnerBill
                )
            }

            DialogAction.NEUTRAL -> {
                val (paymentReason, vaccineMarkCondition) = if (isOutOfAge) {
                    PaymentModeReason.OutOfAgeIndication to VaccineMarkCondition.OUT_OF_AGE
                } else {
                    PaymentModeReason.ImmunizationsNotCovered to VaccineMarkCondition.NOT_COVERED
                }

                VaccineDialogFlipResults(
                    promptResult = CheckoutPromptResult.SELF_PAY,
                    userSelection = UncoveredDosePromptPresentedMetric.UserSelection.SELF_PAY.displayName,
                    paymentModeReason = paymentReason,
                    vaccineMarkCondition = vaccineMarkCondition,
                    paymentMode = PaymentMode.SelfPay
                )
            }

            DialogAction.CANCEL -> {
                dialogIssueMachine.cancelPendingIssues()
                VaccineDialogFlipResults(
                    promptResult = CheckoutPromptResult.REMOVE_DOSE,
                    userSelection = UncoveredDosePromptPresentedMetric.UserSelection.REMOVE_DOSE.displayName,
                    paymentModeReason = PaymentModeReason.Unknown,
                    vaccineMarkCondition = VaccineMarkCondition.MISSING_INFO,
                    paymentMode = PaymentMode.NoPay
                )
            }
        }.let { result ->

            if (result.promptResult != CheckoutPromptResult.REMOVE_DOSE) {
                vaccineWithIssues.overridePaymentMode(
                    newPaymentMode = result.paymentMode,
                    newPaymentModeReason = result.paymentModeReason,
                    newVaccineMarkCondition = result.vaccineMarkCondition
                )
            }

            if (isOutOfAge) {
                saveOutOfAgeIndicationMetric(
                    appointmentId = appointment.id,
                    promptResult = result.promptResult
                )
            } else {
                saveUncoveredDoseMetric(
                    appointmentId = appointment.id,
                    userSelection = result.userSelection
                )
            }

            saveNotCoveredDialogResultMetric(appointment, result.promptResult)
        }

        return ProductPendingUserAction.NONE
    }

    private fun saveNotCoveredDialogResultMetric(appointment: Appointment, metricResult: CheckoutPromptResult) {
        val metric =
            if (appointment.encounterState?.medDMessage?.callToAction == CallToAction.None) {
                CopayCheckUnavailableValidationMetric(
                    visitId = appointment.id,
                    copayCheckUnavailableResult = metricResult
                )
            } else {
                DoseNotCoveredValidationMetric(
                    visitId = appointment.id,
                    doseNotCoveredResult = metricResult
                )
            }

        analytics.saveMetric(metric)
    }

    private fun saveUncoveredDoseMetric(appointmentId: Int, userSelection: String) {
        analytics.saveMetric(UncoveredDosePromptPresentedMetric(appointmentId, userSelection))
    }

    private fun saveOutOfAgeIndicationMetric(appointmentId: Int, promptResult: CheckoutPromptResult) {
        analytics.saveMetric(OutOfAgeIndicationValidationMetric(appointmentId, promptResult))
    }

    private fun VaccineWithIssues.overridePaymentMode(
        newPaymentMode: PaymentMode?,
        newPaymentModeReason: PaymentModeReason? = null,
        newVaccineMarkCondition: VaccineMarkCondition? = null
    ) = apply {
        originalPaymentMode = newPaymentMode
        paymentMode = newPaymentMode
        paymentModeReason = newPaymentModeReason
        vaccineMarkCondition = newVaccineMarkCondition
    }

    /**
     * Class for passing specific meta data for a dialog result and parsed members to apply to a
     * product
     *
     * @property promptResult result of the dialog
     * @property userSelection user selection string for dialog result
     * @property paymentModeReason resulting paymentModeReason to apply to the product
     * @property vaccineMarkCondition resulting vaccineMarkCondition to apply to the product
     * @property paymentMode resulting paymentMode to apply to the product
     */
    data class VaccineDialogFlipResults(
        val promptResult: CheckoutPromptResult,
        val userSelection: String,
        val paymentModeReason: PaymentModeReason,
        val vaccineMarkCondition: VaccineMarkCondition,
        val paymentMode: PaymentMode
    )
}
