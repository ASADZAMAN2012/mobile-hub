/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.checkout.getValidCopay
import com.vaxcare.vaxhub.model.checkout.isNotCovered
import com.vaxcare.vaxhub.model.enums.EditCheckoutStatus
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.ui.checkout.extensions.checkoutStatus

sealed class ProductNotCoveredRules<T> : BaseRules<T>() {
    override val associatedIssue: ProductIssue = ProductIssue.ProductNotCovered

    data class NotInNetworkMessageValidator(override val comparator: NotInNetworkRuleArgs) :
        ProductNotCoveredRules<NotInNetworkRuleArgs>() {
        /**
         * Evaluates DoseNotCovered based on "Not in Network" primary message, product antigen is
         * copay, the product *has* no copay attached or the patient is not eligible for med D
         */
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            val antigen = productToValidate.product.antigen
            val isMedDAndCovered =
                MedDVaccines.isMedDVaccine(antigen) &&
                    (comparator.medDInfo?.getValidCopay(antigen) == null || comparator.medDInfo?.eligible == false)
            return notInNetworkMessages.contains(comparator.primaryMessage) && isMedDAndCovered
        }
    }

    /**
     * Evaluates of product is not covered based on product and appointment status
     *
     *(original docs)
     * Product qualifies for "not covered" when the following criteria is met:
     *  - Patient Plan is Out of Network
     *      - product is copay covered
     *      - MedD has ran > patient not covered
     *  - Risk is any form of Med D
     *  - PaymentMethod is insurance with any risk Med D reject codes
     *  - Payment Method is PartnerBill and copay has been reviewed and they are ineligible
     *  - Appointment or risk CallToAction are reporting as "missing info"
     *  - Add a dose that is not covered we are shown the dose not covered pop up - on an edit
     *  checkout we should not show the pop up. it should be partner bill and should not ask for
     *  payment mode again.
     *
     *  aside from the above, the product antigen should not be covered, and should either have
     *  no copay attached or medDCheck eligibility flag is false
     */
    data class ProductNotCoveredValidator(override val comparator: ProductNotCoveredRuleArgs) :
        ProductNotCoveredRules<ProductNotCoveredRuleArgs>() {
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            return with(comparator) {
                val medDRanAndIneligible =
                    medDInfo.isNotCovered() || medDInfo?.copays.isNullOrEmpty()
                val appointmentIsPartnerBill =
                    appointment.paymentMethod == PaymentMethod.PartnerBill
                val isEditCheckoutForPartnerBill =
                    appointmentIsPartnerBill && appointment.checkoutStatus() == EditCheckoutStatus.PAST_CHECKOUT
                val isCoveredInventoryGroup =
                    coveredInventoryGroup.contains(productToValidate.product.inventoryGroup)

                appointment.paymentMethod != PaymentMethod.SelfPay &&
                    canAppointmentChoosePaymentMode(medDRanAndIneligible) &&
                    (medDInfo?.getValidCopay(productToValidate.product.antigen) == null || medDRanAndIneligible) &&
                    !(isCoveredInventoryGroup || productToValidate.product.isFluOrSeasonal()) &&
                    !isEditCheckoutForPartnerBill
            }
        }

        private fun ProductNotCoveredRuleArgs.canAppointmentChoosePaymentMode(medDRanAndIneligible: Boolean): Boolean {
            val isMedDCanRun = medDInfo == null && appointment.isMedDTagShown()
            return appointment.isPrivate() &&
                (
                    isMedDCanRun || (
                        isInsurancePayAndMedDRejectCode() ||
                            (
                                appointment.paymentMethod == PaymentMethod.PartnerBill &&
                                    medDRanAndIneligible
                            )
                    )
                )
        }

        /**
         * returns true if Appointment PaymentMethod is InsurancePay and the encounter state's
         * vaccine message's topRejectCode is matching the pre-defined reject codes relating to
         * Med D.
         */
        private fun ProductNotCoveredRuleArgs.isInsurancePayAndMedDRejectCode() =
            appointment.paymentMethod == PaymentMethod.InsurancePay &&
                riskRejectCodes.contains(appointment.encounterState?.vaccineMessage?.topRejectCode)
    }

    /**
     * Evaluates DoseNotCovered based on reject code and salesProductId
     *
     * @property comparator - incoming reject code from vaccineMessage or riskAssessment
     */
    data class RejectCodesValidator(override val comparator: RejectCodesRuleArgs) :
        ProductNotCoveredRules<RejectCodesRuleArgs>() {
        /**
         * Evaluates if a product is not covered according to specific RejectCodes and antigen
         */
        override fun validate(productToValidate: LotNumberWithProduct): Boolean {
            val grouping = rejectCodeToAntigenGroupings.filter { it.first == comparator.rejectCode }
            return !comparator.appointmentCheckedOut &&
                grouping.isNotEmpty() && grouping.firstOrNull()
                    ?.second?.contains(productToValidate.product.antigen) == true
        }
    }

    /**
     * NotInNetworkMessageValidator arguments
     *
     * @property primaryMessage - primary message from encounterMessage
     * @property medDInfo - medD data for appointment
     */
    data class NotInNetworkRuleArgs(
        val primaryMessage: String?,
        val medDInfo: MedDInfo?
    )

    /**
     * ProductNotCoveredValidator arguments
     *
     * @property appointment - associated appointment
     * @property medDInfo - medD data for appointment
     */
    data class ProductNotCoveredRuleArgs(
        val appointment: Appointment,
        val medDInfo: MedDInfo?
    )

    data class RejectCodesRuleArgs(
        val rejectCode: String?,
        val appointmentCheckedOut: Boolean
    )
}
