/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory.validator

import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.getInventorySource
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.SimpleOnHandProduct
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProductVerifier(private val features: HubFeatures) {
    fun getProductIssues(
        productToCheck: LotNumberWithProduct,
        appointment: Appointment,
        stagedProducts: List<VaccineAdapterDto>,
        doseSeries: Int?,
        manualDob: LocalDate? = null,
        initialDoseState: DoseState = DoseState.ADDED,
        simpleOnHandProductInventory: List<SimpleOnHandProduct>,
        isVaxCare3: Boolean,
        medDInfo: MedDInfo?
    ): VaccineWithIssues? {
        val sources = simpleOnHandProductInventory
            .filter { it.lotNumberName == productToCheck.name }
            .map { it.inventorySource }
        val builder = VaccineWithIssues.Builder()
            .lotNumberWithProduct(productToCheck)
            .doseSeries(doseSeries)
            .doseState(initialDoseState)
            .paymentMode(
                checkForCopaysOverridePaymentMode(
                    medDCheckEligible = medDInfo?.eligible ?: false,
                    appointmentPaymentMode = appointment.paymentMethod.toPaymentMode(),
                    productCopay = productToCheck.copay,
                    isVaxCare3 = isVaxCare3
                )
            )
            .appointmentPaymentMethod(appointment.paymentMethod)
            .sources(*sources.toTypedArray())

        val validators = getListOfValidators(
            appointment = appointment,
            productToCheck = productToCheck,
            stagedProducts = stagedProducts,
            manualDob = manualDob,
            simpleOnHandProductInventory = simpleOnHandProductInventory,
            medDInfo = medDInfo
        )

        validators.forEach { rule ->
            if (rule.validate(productToCheck)) {
                builder.issues(rule.associatedIssue)
            }
        }

        return builder.build()
    }

    private fun getListOfValidators(
        appointment: Appointment,
        productToCheck: LotNumberWithProduct,
        stagedProducts: List<VaccineAdapterDto>,
        manualDob: LocalDate?,
        simpleOnHandProductInventory: List<SimpleOnHandProduct>,
        medDInfo: MedDInfo?
    ) = listOf(
        RequiresCopayReviewRules.ProductRequiresCopayValidator(
            RequiresCopayReviewRules.ProductRequiresCopayRuleArgs(
                isMedDVisit = appointment.isMedDTagShown(),
                medDInfo = medDInfo,
                dos = appointment.appointmentTime.toLocalDate()
            )
        ),
        NotOrderedRules.ProductNotOrderedValidator(
            NotOrderedRules.ProductNotOrderedRuleArgs(
                rprd = features.isRprdAndNotLocallyCreated,
                orders = appointment.orders
            )
        ),
        MissingLotNumberRules.ProductMissingLotNumberValidator(
            productToCheck.name
        ),
        DoseExpiredRules.DoseExpiredValidator(
            productToCheck.expirationDate
        ),
        OutOfAgeWarningRules.AgeWarningValidator(
            OutOfAgeWarningRules.OutOfAgeWarningArgs(
                patientGender = appointment.patient.gender,
                patientDoB = manualDob ?: appointment.patient.getDobString()?.let {
                    LocalDate.parse(
                        it,
                        DateTimeFormatter.ofPattern("M/dd/yyyy")
                    )
                }
            )
        ),
        AgeIndicatedRules.AgeIndicatedValidator(
            AgeIndicatedRules.AgeIndicatedArgs(
                patientGender = appointment.patient.gender,
                patientDoB = manualDob ?: appointment.patient.getDobString()?.let {
                    LocalDate.parse(
                        it,
                        DateTimeFormatter.ofPattern("M/dd/yyyy")
                    )
                }
            )
        ),
        ProductNotCoveredRules.NotInNetworkMessageValidator(
            ProductNotCoveredRules.NotInNetworkRuleArgs(
                primaryMessage = appointment.encounterState?.vaccinePrimaryMessage,
                medDInfo = medDInfo
            )
        ),
        ProductNotCoveredRules.ProductNotCoveredValidator(
            ProductNotCoveredRules.ProductNotCoveredRuleArgs(
                appointment = appointment,
                medDInfo = medDInfo
            )
        ),
        ProductNotCoveredRules.RejectCodesValidator(
            ProductNotCoveredRules.RejectCodesRuleArgs(
                rejectCode = appointment.encounterState?.vaccineMessage?.topRejectCode,
                appointmentCheckedOut = appointment.checkedOut
            )
        ),
        DuplicateProductExceptionRules.DuplicateRSVValidator(
            DuplicateProductExceptionRules.DuplicateProductExceptionRuleArgs(
                stagedProducts = stagedProducts,
                isDisableDuplicateRSV = features.isDisableDuplicateRSV,
                patientDoB = manualDob ?: appointment.patient.getDobString()?.let {
                    LocalDate.parse(
                        it,
                        DateTimeFormatter.ofPattern("M/dd/yyyy")
                    )
                }
            )
        ),
        ConflictingProductRules.DuplicateLotValidator(
            ConflictingProductRules.ConflictingProductRuleArgs(
                stagedProducts = stagedProducts
            )
        ),
        ConflictingProductRules.DuplicateProductValidator(
            ConflictingProductRules.ConflictingProductRuleArgs(
                stagedProducts = stagedProducts
            ),
        ),
        RouteSelectionRules.RouteSelectionRequiredValidator(
            RouteSelectionRules.RouteSelectionArgs(
                product = productToCheck.product
            )
        ),
        WrongStockRules.WrongStockValidator(
            WrongStockRules.WrongStockArgs(
                appointmentInventorySource = appointment.getInventorySource(),
                simpleOnHandInventory = simpleOnHandProductInventory
            )
        ),
        LarcAddedRules.LarcAddedValidator(ProductCategory.LARC)
    )

    /**
     * Set PaymentMode to override as InsurancePay if there is a valid copay and the patient has
     * eligibility. It is important we set this as null if the paymentMode matches the appointment
     * level.
     *
     * @param appointmentPaymentMode - appointment paymentmode
     * @param productCopay - product copay populated after valid medd check
     * @return the overridden PaymentMode if applicable
     */
    private fun checkForCopaysOverridePaymentMode(
        medDCheckEligible: Boolean,
        appointmentPaymentMode: PaymentMode,
        productCopay: ProductCopayInfo?,
        isVaxCare3: Boolean
    ): PaymentMode? =
        if (isVaxCare3 &&
            productCopay != null &&
            medDCheckEligible &&
            appointmentPaymentMode != PaymentMode.InsurancePay
        ) {
            PaymentMode.InsurancePay
        } else {
            null
        }
}

data class HubFeatures(
    val isRprdAndNotLocallyCreated: Boolean,
    val isDisableDuplicateRSV: Boolean
)
