/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.COVID
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.COVID_19
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.FLU
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.IPOL
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.MMR_II
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.PNEUMOVAX23
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.PRO_QUAD
import com.vaxcare.vaxhub.core.constant.AntigensWithSpecialAccommodations.VARIVAX
import com.vaxcare.vaxhub.core.span.FontSpan
import com.vaxcare.vaxhub.model.enums.ProductCategory
import com.vaxcare.vaxhub.model.enums.ProductPresentation
import com.vaxcare.vaxhub.model.enums.ProductStatus
import com.vaxcare.vaxhub.model.enums.RouteCode

abstract class BaseProduct {
    abstract val antigen: String
    abstract val categoryId: ProductCategory
    abstract val description: String
    abstract val displayName: String
    abstract val id: Int
    abstract val inventoryGroup: String
    abstract val lossFee: Int?
    abstract val productNdc: String?
    abstract val routeCode: RouteCode
    abstract val presentation: ProductPresentation
    abstract val purchaseOrderFee: Int?
    abstract val visDates: String?
    abstract val status: ProductStatus
    abstract var prettyName: String?

    @Ignore
    open val ageIndications: List<AgeIndication> = listOf()

    @Ignore
    open val packages: List<Package> = listOf()

    @Ignore
    open val cptCvxCodes: List<CptCvxCode> = listOf()

    fun displaySpannable(context: Context): Spannable {
        val index = displayName.indexOf("(")
        val sb = SpannableStringBuilder(displayName)
        val style = FontSpan(context.resources.getFont(R.font.graphik_semi_bold))
        val style2 = FontSpan(context.resources.getFont(R.font.graphik_regular))

        if (index == -1) {
            sb.setSpan(
                style,
                0,
                displayName.length - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } else {
            sb.setSpan(
                style,
                0,
                index - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            sb.setSpan(
                style2,
                index,
                displayName.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
        return sb
    }

    fun displaySpannableForCheckout(context: Context): Spannable {
        val index = displayName.indexOf("(")
        val sb = SpannableStringBuilder(displayName)
        val style = TextAppearanceSpan(context, R.style.H6BoldBlack)
        val style2 = TextAppearanceSpan(context, R.style.H6RegBlack)

        if (index == -1) {
            sb.setSpan(
                style,
                0,
                displayName.length - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } else {
            sb.setSpan(
                style,
                0,
                index - 1,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
            sb.setSpan(
                style2,
                index,
                displayName.length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
        return sb
    }

    fun getProductIcon(): Int {
        return presentation.icon
    }

    fun isFlu() = status.isFlu() || antigen == FLU
}

/**
 * Check if the product status is related to Flu.
 *
 * @return true if the status meets the above criteria
 */
private fun ProductStatus.isFlu(): Boolean =
    this in listOf(
        ProductStatus.DISABLED,
        ProductStatus.FLU_ENABLED,
        ProductStatus.HISTORICAL,
        ProductStatus.HISTORICAL_FLU
    )

@JsonClass(generateAdapter = true)
data class ProductDto(
    override val antigen: String,
    override val categoryId: ProductCategory,
    override val description: String,
    override val displayName: String,
    @PrimaryKey override val id: Int,
    override val inventoryGroup: String,
    override val lossFee: Int?,
    override val productNdc: String?,
    override val routeCode: RouteCode,
    override val presentation: ProductPresentation,
    override val purchaseOrderFee: Int?,
    override val visDates: String?,
    @Json(name = "statusId") override val status: ProductStatus,
    override val ageIndications: List<AgeIndication>,
    override val cptCvxCodes: List<CptCvxCode>,
    override val packages: List<Package>,
    override var prettyName: String?
) : BaseProduct()

@Entity
@JsonClass(generateAdapter = true)
data class Product(
    override val antigen: String,
    override val categoryId: ProductCategory,
    override val description: String,
    override val displayName: String,
    @PrimaryKey override val id: Int,
    override val inventoryGroup: String,
    override val lossFee: Int?,
    override val productNdc: String?,
    override var routeCode: RouteCode,
    override val presentation: ProductPresentation,
    override val purchaseOrderFee: Int?,
    override val visDates: String?,
    @Json(name = "statusId") override val status: ProductStatus,
    override var prettyName: String?
) : BaseProduct() {
    companion object {
        val routeAntigens = listOf(IPOL, PNEUMOVAX23, PRO_QUAD, MMR_II, VARIVAX)
        val covidAntigens = listOf(COVID, COVID_19)
    }

    fun needsRouteSelection(): Boolean = antigen in routeAntigens

    fun isCovid() = antigen.uppercase() in covidAntigens

    fun isLegacyCovid() = antigen.uppercase() == COVID

    fun isFluOrSeasonal(): Boolean = this.isFlu() || this.isCovid()

    @Ignore
    var orderReasonContext: DoseReasonContext? = null
}

@Entity
@JsonClass(generateAdapter = true)
data class Package(
    val description: String,
    @PrimaryKey val id: Int,
    val itemCount: Int,
    val productId: Int
) {
    @Ignore
    var packageNdcs: List<String> = listOf()
}

@Entity
@JsonClass(generateAdapter = true)
data class CptCvxCode(
    @PrimaryKey val cptCode: String,
    val cvxCode: String?,
    val isMedicare: Boolean,
    val productId: Int
)

@Entity
data class NdcCode(
    @PrimaryKey val ndcCode: String,
    val productId: Int
)

data class ProductAndPackage(
    override val antigen: String,
    override val categoryId: ProductCategory,
    override val description: String,
    override val displayName: String,
    override val id: Int,
    override val lossFee: Int?,
    override val inventoryGroup: String,
    override val productNdc: String?,
    override val presentation: ProductPresentation,
    override val purchaseOrderFee: Int?,
    override val routeCode: RouteCode,
    override val status: ProductStatus,
    override val visDates: String?,
    override var prettyName: String?,
    @Relation(parentColumn = "id", entityColumn = "productId")
    override val packages: List<Package>,
    @Relation(parentColumn = "id", entityColumn = "productId")
    override val cptCvxCodes: List<CptCvxCode>,
    @Relation(parentColumn = "id", entityColumn = "productId")
    override val ageIndications: List<AgeIndication>
) : BaseProduct() {
    fun toProduct() =
        Product(
            antigen = antigen,
            categoryId = categoryId,
            description = description,
            displayName = displayName,
            id = id,
            lossFee = lossFee,
            inventoryGroup = inventoryGroup,
            productNdc = productNdc,
            routeCode = routeCode,
            presentation = presentation,
            purchaseOrderFee = purchaseOrderFee,
            status = status,
            visDates = visDates,
            prettyName = prettyName
        )
}
