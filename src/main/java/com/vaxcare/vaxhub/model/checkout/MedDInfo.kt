/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.checkout

import android.content.Context
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.model.enums.PartDEligibilityStatusCode
import com.vaxcare.vaxhub.model.inventory.Product
import com.vaxcare.vaxhub.model.partd.PartDCopay
import com.vaxcare.vaxhub.model.partd.PartDResponse
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Parcelize
data class MedDInfo(
    val eligible: Boolean,
    val copays: List<ProductCopayInfo>
) : Parcelable {
    fun hasCoveredCopays() = copays.any { it.isCovered() }

    fun areCopaysNotCovered() = copays.all { !it.isCovered() }

    fun areCopaysError() = copays.all { it.isFailure() }
}

fun MedDInfo?.getValidCopay(antigen: String) =
    this?.copays
        ?.filter { copay -> copay.isCovered() }
        ?.firstOrNull { copay -> copay.antigen.value == antigen }

@OptIn(ExperimentalContracts::class)
fun MedDInfo?.hasCovered(): Boolean {
    contract {
        returns(true) implies (this@hasCovered != null)
    }

    return this != null && hasCoveredCopays()
}

@OptIn(ExperimentalContracts::class)
fun MedDInfo?.isNotCovered(): Boolean {
    contract {
        returns(true) implies (this@isNotCovered != null)
    }

    return this != null && areCopaysNotCovered()
}

fun MedDInfo?.isError(): Boolean = this != null && areCopaysError()

@Parcelize
data class ProductCopayInfo(
    val antigen: MedDVaccines,
    val copay: BigDecimal,
    val ndcCode: String? = null,
    val productName: String? = null,
    val notFoundMessage: Int? = null,
    val eligibilityStatusCode: PartDEligibilityStatusCode? = null,
    val requestStatus: Int? = null,
    val coveredProductIds: String? = null
) : Parcelable {
    fun getDisplayString(resourceBuilder: (Int) -> String): String =
        notFoundMessage?.let { resourceBuilder(it) } ?: "$${copay.setScale(2)}"

    fun isCovered() = eligibilityStatusCode == PartDEligibilityStatusCode.EligibilityVerified

    fun isFailure() = PartDEligibilityStatusCode.isFailure(eligibilityStatusCode)
}

fun ProductCopayInfo.getDisplaySpannable(context: Context): Spannable {
    val displayName = if (productName == null) {
        antigen.value
    } else {
        "${antigen.value} ($productName)"
    }
    val index = displayName.indexOf("(")
    val sb = SpannableStringBuilder(displayName)
    val boldStyle = TextAppearanceSpan(context, R.style.H6BoldBlack)
    val regStyle = TextAppearanceSpan(context, R.style.H6RegBlack)

    if (index == -1) {
        sb.setSpan(
            boldStyle,
            0,
            displayName.length - 1,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    } else {
        sb.setSpan(
            boldStyle,
            0,
            index - 1,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        sb.setSpan(
            regStyle,
            index,
            displayName.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    }
    return sb
}

fun PartDResponse.toMedDInfo(
    notFoundMessageResId: Int?,
    notCoveredMessageResId: Int?,
    converter: (List<PartDCopay>) -> Map<String, List<Pair<Product, PartDCopay>>>
): MedDInfo {
    val allProducts = converter(copays)
    val coveredProducts = converter(copays.filter { it.isEligible() })
    val notCoveredProducts = converter(copays.filter { !it.isEligible() })

    val coveredCopays = coveredProducts.mapNotNull { (antigen, productsAndCopays) ->
        val coveredProductIds = productsAndCopays
            .mapNotNull { it.second.productId }
            .joinToString(",")
        val (product, copay) = productsAndCopays.first()
        ProductCopayInfo(
            antigen = MedDVaccines.getByAntigen(antigen),
            copay = copay.copay ?: BigDecimal.ZERO,
            productName = product.prettyName,
            ndcCode = copay.ndc ?: "-",
            eligibilityStatusCode = copay.eligibilityStatusCode,
            requestStatus = copay.requestStatus,
            coveredProductIds = coveredProductIds
        )
    }

    val notCoveredCopays = notCoveredMessageResId?.let { notCoveredRes ->
        notCoveredProducts.mapNotNull { (antigen, productsAndCopays) ->
            val (product, copay) = productsAndCopays.first()
            ProductCopayInfo(
                antigen = MedDVaccines.getByAntigen(antigen),
                copay = BigDecimal.ZERO,
                productName = product.prettyName,
                notFoundMessage = notCoveredRes,
                eligibilityStatusCode = copay.eligibilityStatusCode,
                requestStatus = copay.requestStatus
            )
        }
    } ?: emptyList()

    val notFoundCopays = notFoundMessageResId?.let { notFoundRes ->
        val allAntigens = allProducts.map { it.key.uppercase() }
        MedDVaccines.values()
            .filter { vac ->
                vac != MedDVaccines.UNKNOWN && vac.value.uppercase() !in allAntigens
            }
            .let { missingAntigens ->
                extractMissingCopays(
                    missingAntigens = missingAntigens,
                    allProducts = allProducts,
                    messageResId = notFoundRes
                )
            }
    } ?: emptyList()

    val resultCopays =
        (coveredCopays + notCoveredCopays + notFoundCopays)
            .groupBy { it.antigen.value }
            .map { (_, copay) -> copay.first() }
            .sortedBy { it.antigen.ordinal }

    return MedDInfo(
        eligible = copays.any { it.isEligible() },
        copays = resultCopays
    )
}

private fun PartDResponse.extractMissingCopays(
    missingAntigens: List<MedDVaccines>,
    allProducts: Map<String, List<Pair<Product, PartDCopay>>>,
    messageResId: Int
) = missingAntigens.map { missingAntigen ->
    val eligibilityStatusCode = allProducts
        .firstNotNullOfOrNull { (antigen, pair) ->
            val matchingAntigen =
                antigen.uppercase() == missingAntigen.value.uppercase()
            if (matchingAntigen) {
                val (eligibleIds, eligibleCopays) = pair.map { it.first.id } to pair.map { it.second }
                val filteredCopays =
                    copays - eligibleCopays.firstOrNull { it.productId in eligibleIds }
                filteredCopays.firstOrNull()
            } else {
                null
            }
        }?.eligibilityStatusCode
    ProductCopayInfo(
        antigen = missingAntigen,
        copay = BigDecimal.ZERO,
        notFoundMessage = messageResId,
        eligibilityStatusCode = eligibilityStatusCode
    )
}
