/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import androidx.annotation.DrawableRes
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.R

@JsonClass(generateAdapter = false)
enum class ProductCategory(val id: Int) {
    UNKNOWN(0),
    SUPPLY(1),
    VACCINE(2),
    LARC(3);

    companion object {
        private val map = values().associateBy(ProductCategory::id)

        fun fromInt(type: Int) = map[type] ?: UNKNOWN
    }
}

@JsonClass(generateAdapter = false)
enum class ProductStatus(val id: Int) {
    DISABLED(0),
    VACCINE_ENABLED(1),
    FLU_ENABLED(2),
    HISTORICAL(4),
    HISTORICAL_VACCINE(5),
    HISTORICAL_FLU(6);

    companion object {
        private val map = values().associateBy(ProductStatus::id)

        fun fromInt(type: Int) = map[type] ?: DISABLED
    }
}

@JsonClass(generateAdapter = false)
enum class RouteCode(val displayName: String) {
    IM("Intramuscular"),
    ID("Intradermal"),
    NS("Nasal"),
    PO("Periodontal"),
    SC("Subcutaneous"),
    IUD("Intrauterine"),
    IMP("Implant"),
    UNKNOWN("Unknown");

    companion object {
        private val map = values().associateBy(RouteCode::ordinal)

        fun fromInt(type: Int) = map[type] ?: UNKNOWN

        fun fromString(name: String?): RouteCode? =
            when (name) {
                IM.displayName, IM.name -> IM
                ID.displayName, ID.name -> ID
                NS.displayName, NS.name -> NS
                PO.displayName, PO.name -> PO
                SC.displayName, SC.name -> SC
                IUD.displayName, IUD.name -> IUD
                IMP.displayName, IMP.name -> IMP
                UNKNOWN.displayName -> UNKNOWN
                else -> null
            }
    }
}

@JsonClass(generateAdapter = false)
enum class ProductPresentation(
    val longName: String,
    val shortName: String,
    @DrawableRes val icon: Int
) {
    @Json(name = "Single Dose Vial")
    SINGLE_DOSE_VIAL("Single Dose Vial", "SDV", R.drawable.ic_presentation_single_dose_vial),

    @Json(name = "Single Dose Tube")
    SINGLE_DOSE_TUBE("Single Dose Tube", "SDT", R.drawable.ic_presentation_oral),

    @Json(name = "Multi Dose Vial")
    MULTI_DOSE_VIAL("Multi Dose Vial", "MDV", R.drawable.ic_presentation_multi_dose_vial),

    @Json(name = "Prefilled Syringe")
    PREFILLED_SYRINGE("Prefilled Syringe", "PFS", R.drawable.ic_presentation_prefilled_syringe),

    @Json(name = "Nasal Spray")
    NASAL_SPRAY("Nasal Spray", "NSP", R.drawable.ic_presentation_nasal),

    @Json(name = "Nasal Syringe")
    NASAL_SYRINGE("Nasal Syringe", "NSY", R.drawable.ic_presentation_nasal),

    @Json(name = "IUD")
    IUD("Intrauterine Device", "IUD", R.drawable.ic_presentation_iud),

    @Json(name = "Implant")
    IMPLANT("Etonogestrel Single-Rod Contraceptive Implant", "IMP", R.drawable.ic_presentation_implant),
    UNKNOWN("Unknown", "UNK", R.drawable.ic_presentation_single_dose_vial);

    companion object {
        private val map = values().associateBy(ProductPresentation::ordinal)

        fun fromInt(type: Int) = map[type] ?: UNKNOWN
    }
}
