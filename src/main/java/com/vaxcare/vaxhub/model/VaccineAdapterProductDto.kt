/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.VaccineMarkCondition
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.inventory.ProductOneTouch
import com.vaxcare.vaxhub.model.inventory.Site
import com.vaxcare.vaxhub.model.order.OrderEntity
import java.math.BigDecimal
import java.util.UUID

enum class DoseState {
    /**
     * The dose has been added to the cart
     */
    ADDED,

    /**
     * The dose was recently added and has been marked for deletion
     */
    REMOVED,

    /**
     * The dose was already administered from a past checkout
     */
    ADMINISTERED,

    /**
     * The dose was already administered from a past checkout and is marked for deletion
     */
    ADMINISTERED_REMOVED,

    /**
     * The Dose is an order and no doses have been scanned to fulfill it
     */
    ORDERED
}

interface VaccineAdapterDto {
    val id: String
    val product: Product
    val weight: Int
    var hasEdit: Boolean
    val salesProductId: Int
    var doseState: DoseState
    var orderNumber: String?
    var isDeleted: Boolean

    fun isRemoveDoseState(): Boolean {
        return doseState in listOf(
            DoseState.ADMINISTERED_REMOVED,
            DoseState.ORDERED,
            DoseState.REMOVED
        )
    }
}

data class VaccineAdapterOrderDto(
    override val id: String = UUID.randomUUID().toString(),
    override var hasEdit: Boolean = false,
    override var weight: Int = 100,
    override val product: Product,
    override val salesProductId: Int,
    override var doseState: DoseState = DoseState.ORDERED,
    override var orderNumber: String?,
    var order: OrderEntity,
    override var isDeleted: Boolean = false
) : VaccineAdapterDto

class VaccineAdapterProductDto(
    override val id: String = UUID.randomUUID().toString(),
    val productId: Int,
    override val product: Product,
    val lotNumber: String,
    val method: String? = null,
    var doseSeries: Int? = null,
    val dosesInSeries: Int,
    var copay: ProductCopayInfo? = null,
    var oneTouch: ProductOneTouch? = null,
    var paymentMode: PaymentMode? = null,
    var paymentModeReason: PaymentModeReason? = null,
    var originalPaymentMode: PaymentMode? = null,
    var originalPaymentModeReason: PaymentModeReason? = null,
    override var isDeleted: Boolean = false,
    var appointmentPaymentMethod: PaymentMethod,
    override val salesProductId: Int,
    override var orderNumber: String? = null,
    override var weight: Int = 10,
    var unorderReasonId: Int? = null,
    var missingInsuranceSelfPay: Boolean = false,
    var missingInsurancePartnerBill: Boolean = false,
    var cashCheckPartnerBill: Boolean = false,
    var vaccineIssues: Set<ProductIssue> = emptySet(),
    var paymentModeRevertValues: PaymentModeRevertValues? = null
) : VaccineAdapterDto {
    constructor(
        lotNumberWithProduct: LotNumberWithProduct,
        doseSeries: Int?,
        paymentMode: PaymentMode? = null,
        appointmentPaymentMethod: PaymentMethod,
    ) : this(
        productId = lotNumberWithProduct.productId,
        salesProductId = lotNumberWithProduct.salesProductId,
        product = lotNumberWithProduct.product,
        lotNumber = lotNumberWithProduct.name,
        method = lotNumberWithProduct.product.routeCode.name,
        doseSeries = doseSeries,
        dosesInSeries = lotNumberWithProduct.dosesInSeries,
        copay = lotNumberWithProduct.copay,
        oneTouch = lotNumberWithProduct.oneTouch,
        paymentMode = paymentMode,
        originalPaymentMode = paymentMode,
        appointmentPaymentMethod = appointmentPaymentMethod,
    )

    val hasDisplayIssues: Boolean
        get() = vaccineIssues.any {
            it in listOf(
                ProductIssue.Expired,
                ProductIssue.OutOfAgeIndication,
                ProductIssue.RestrictedProduct,
                ProductIssue.WrongStock
            )
        }
    val doseExpired: Boolean
        get() = vaccineIssues.contains(ProductIssue.Expired)
    val ageIndicated: Boolean
        get() = vaccineIssues.contains(ProductIssue.OutOfAgeIndication)
    val isRestrictedProduct: Boolean
        get() = vaccineIssues.contains(ProductIssue.RestrictedProduct)
    val isWrongStock: Boolean
        get() = vaccineIssues.contains(ProductIssue.WrongStock)

    var swipeReset: Boolean = false
    var site: Site.SiteValue? = null
    var vaccineMarkCondition: VaccineMarkCondition? = null
    var medDRiskRunStatus: MedDRiskRunStatus? = null

    override var hasEdit: Boolean = false
    override var doseState: DoseState =
        if (isDeleted) DoseState.ADMINISTERED_REMOVED else DoseState.ADDED

    fun isLegacyCovid(): Boolean = product.isLegacyCovid()

    fun needsRouteSelection(): Boolean = product.needsRouteSelection()

    fun hasCopay(ignoreTotal: Boolean = false): Boolean =
        copay?.copay?.let { ignoreTotal || it > BigDecimal.ZERO } ?: false

    fun isSelfPayAndNonZeroRate(): Boolean = paymentMode == PaymentMode.SelfPay && hasNonZeroSelfPayRate()

    fun hasCopaysAndNotMissingPartnerBill(ignoreCopaysTotal: Boolean): Boolean =
        hasCopay(ignoreCopaysTotal) && !missingInsurancePartnerBill

    private fun hasNonZeroSelfPayRate(): Boolean = oneTouch?.selfPayRate?.let { it > BigDecimal.ZERO } ?: false

    fun isAnyLotAdded(): Boolean {
        return doseState == DoseState.ADDED || (isDeleted && doseState == DoseState.ADMINISTERED)
    }

    fun isAnyLotRemoved(): Boolean {
        return !isDeleted && doseState == DoseState.ADMINISTERED_REMOVED
    }

    fun flipSelfPay(value: Boolean) {
        missingInsuranceSelfPay = value
        paymentMode = if (value) {
            PaymentMode.SelfPay
        } else {
            originalPaymentMode
        }
    }

    fun flipPartnerBill(value: Boolean) {
        missingInsurancePartnerBill = value
        paymentMode = if (value) {
            PaymentMode.PartnerBill
        } else {
            originalPaymentMode
        }
    }

    fun overridePaymentModeAndOriginalPaymentMode(value: PaymentMode) {
        paymentMode = value
        originalPaymentMode = value
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

    fun restorePaymentModeRevertValuesAfterDeletingDuplicate() {
        originalPaymentMode = paymentModeRevertValues?.originalPaymentMode
        paymentMode = paymentModeRevertValues?.paymentMode
        paymentModeReason = paymentModeRevertValues?.paymentModeReason
        vaccineMarkCondition = paymentModeRevertValues?.vaccineMarkCondition
    }

    fun isSameProduct(otherProduct: VaccineAdapterDto) =
        id != otherProduct.id && salesProductId == otherProduct.salesProductId

    fun isSelfPayDose() =
        paymentMode == PaymentMode.SelfPay || (paymentMode == null && appointmentPaymentMethod == PaymentMethod.SelfPay)
}

data class PaymentModeRevertValues(
    val originalPaymentMode: PaymentMode?,
    val paymentMode: PaymentMode?,
    val paymentModeReason: PaymentModeReason?,
    val vaccineMarkCondition: VaccineMarkCondition?
)
