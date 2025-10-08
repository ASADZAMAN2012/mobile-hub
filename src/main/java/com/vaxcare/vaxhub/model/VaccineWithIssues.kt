/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.enums.VaccineMarkCondition
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Site

class VaccineWithIssues private constructor(
    val lotNumberWithProduct: LotNumberWithProduct,
    val issues: MutableSet<ProductIssue>,
    var paymentMode: PaymentMode?,
    val appointmentPaymentMethod: PaymentMethod,
    val doseState: DoseState,
    val doseSeries: Int?,
    var sources: List<InventorySource>,
    var paymentModeRevertValues: PaymentModeRevertValues? = null
) {
    data class Builder(
        var lotNumberWithProduct: LotNumberWithProduct? = null,
        var issues: MutableSet<ProductIssue> = mutableSetOf(),
        var appointmentPaymentMethod: PaymentMethod = PaymentMethod.InsurancePay,
        var paymentMode: PaymentMode? = PaymentMode.InsurancePay,
        var doseState: DoseState = DoseState.ADDED,
        var doseSeries: Int? = null,
        var sources: List<InventorySource> = emptyList()
    ) {
        fun lotNumberWithProduct(lotNumberWithProduct: LotNumberWithProduct) =
            apply {
                this.lotNumberWithProduct = lotNumberWithProduct
            }

        fun issues(vararg issues: ProductIssue) = apply { this.issues.addAll(issues) }

        fun appointmentPaymentMethod(appointmentPaymentMethod: PaymentMethod) =
            apply {
                this.appointmentPaymentMethod = appointmentPaymentMethod
            }

        fun paymentMode(paymentMode: PaymentMode?) =
            apply {
                this.paymentMode = paymentMode
            }

        fun doseState(doseState: DoseState?) =
            apply {
                this.doseState = doseState ?: this.doseState
            }

        fun doseSeries(doseSeries: Int?) =
            apply {
                this.doseSeries = doseSeries
            }

        fun sources(vararg sources: InventorySource) =
            apply {
                this.sources = sources.asList()
            }

        fun build() =
            lotNumberWithProduct?.let { product ->
                VaccineWithIssues(
                    product,
                    issues,
                    paymentMode,
                    appointmentPaymentMethod,
                    doseState,
                    doseSeries,
                    sources
                )
            }
    }

    // Accommodate orderNumber for administeredVaccine flow
    var orderNumber: String? = null

    // Accommodate site for administeredVaccine flow
    var site: Site.SiteValue? = null

    var originalPaymentMode: PaymentMode? = null

    var paymentModeReason: PaymentModeReason? = null

    var vaccineMarkCondition: VaccineMarkCondition? = null

    var copay: ProductCopayInfo? = null

    fun hasMissingLotNumber() = issues.contains(ProductIssue.MissingLotNumber)

    fun toAdapterDto() =
        VaccineAdapterProductDto(
            lotNumberWithProduct = lotNumberWithProduct,
            doseSeries = doseSeries,
            paymentMode = paymentMode,
            appointmentPaymentMethod = appointmentPaymentMethod
        ).also { result ->
            result.doseState = doseState
            result.orderNumber = orderNumber
            result.site = site
            result.vaccineIssues = issues
            result.originalPaymentMode = originalPaymentMode
            result.paymentModeReason = paymentModeReason
            result.vaccineMarkCondition = vaccineMarkCondition
            result.copay = copay
            result.paymentModeRevertValues = paymentModeRevertValues
        }

    fun overrideSiteAndRoute(site: String? = null, route: String? = null) {
        site?.let { site ->
            this.site = Site.SiteValue.fromString(site)
        }
        route?.let { route ->
            this.lotNumberWithProduct.product.routeCode = RouteCode.valueOf(route)
        }
    }

    fun savePaymentModeRevertValuesBeforeDuplicating(): PaymentModeRevertValues {
        val revertValues = PaymentModeRevertValues(
            originalPaymentMode = originalPaymentMode,
            paymentMode = paymentMode,
            paymentModeReason = paymentModeReason,
            vaccineMarkCondition = vaccineMarkCondition
        )
        paymentModeRevertValues = revertValues
        return revertValues
    }
}

/**
 * MedD Run Status for Appointment Risk
 *
 * MedDCanNotRun - for fallback/default CTA from medD risk assessment
 * MedDCanRun - for "MedDCanRun" CTA from medD risk assessment
 * MedDRan - for the following CTA values
 * (
 *      CallToAction.MedDCollectCreditCard,
 *      CallToAction.MedDCollectSignature,
 *      CallToAction.None,
 *      CallToAction.MedDDidRun
 * )
 *  - the isCovered boolean is driven by the eligible property from the copays call
 */
sealed class MedDRiskRunStatus {
    object MedDCanNotRun : MedDRiskRunStatus()

    object MedDCanRun : MedDRiskRunStatus()

    class MedDRan(val isCovered: Boolean) : MedDRiskRunStatus()
}
