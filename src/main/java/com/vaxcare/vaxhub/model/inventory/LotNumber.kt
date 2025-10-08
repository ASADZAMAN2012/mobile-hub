/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import java.time.LocalDate

@Entity
@JsonClass(generateAdapter = true)
data class LotNumber(
    val expirationDate: LocalDate?,
    @PrimaryKey(autoGenerate = false) val id: Int,
    @Json(name = "qualifiedLotNumber") val name: String,
    @Json(name = "epProductId") val productId: Int,
    val salesLotNumberId: Int,
    val salesProductId: Int,
    val unreviewed: Boolean,
    val source: Int?
)

@JsonClass(generateAdapter = true)
data class LotNumberRequestBody(
    val expirationDate: LocalDate?,
    val id: Int,
    @Json(name = "qualifiedLotNumber") val name: String,
    @Json(name = "epProductId") val productId: Int,
    val salesLotNumberId: Int,
    val salesProductId: Int,
    val source: Int?
)

data class LotNumberWithProduct(
    val expirationDate: LocalDate?,
    val id: Int,
    val name: String,
    val productId: Int,
    val salesLotNumberId: Int,
    val salesProductId: Int,
    @Relation(parentColumn = "productId", entityColumn = "productId")
    val ageIndications: List<AgeIndication>,
    @Relation(parentColumn = "productId", entityColumn = "productId")
    val cptCodes: List<CptCvxCode>,
    @Relation(parentColumn = "productId", entityColumn = "id")
    val product: Product
) {
    @Ignore
    constructor(
        expirationDate: LocalDate?,
        id: Int,
        name: String,
        productId: Int,
        salesLotNumberId: Int,
        salesProductId: Int,
        ageIndications: List<AgeIndication>,
        cptCodes: List<CptCvxCode>,
        product: Product,
        dosesInSeries: Int
    ) : this(
        expirationDate,
        id,
        name,
        productId,
        salesLotNumberId,
        salesProductId,
        ageIndications,
        cptCodes,
        product
    ) {
        this.dosesInSeries = dosesInSeries
    }

    @Ignore
    var dosesInSeries: Int = 1 // dose is at least once, default to 1

    @Ignore
    var copay: ProductCopayInfo? = null

    @Ignore
    var oneTouch: ProductOneTouch? = null
}

/**
 * Wrapper object for product to pass in raw scan details
 */
data class ProductSelectionDetails(
    var product: Product? = null,
    var symbologyScanned: String = "",
    var rawBarcodeData: String = "",
    var ndc: String = "",
    var expirationDate: String = "",
    var lotNumber: String = "",
    var saveMetric: Boolean = true
)
