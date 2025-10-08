/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.inventory

object Site {
    enum class SiteValue(val displayName: String, val truncatedName: String, val abbreviation: String) {
        LEFT_ARM("Arm - Left", "Arm - Left", "LA"),
        RIGHT_ARM("Arm - Right", "Arm - Right", "RA"),
        LEFT_DELTOID("Deltoid - Left", "Deltoid - Left", "LD"),
        RIGHT_DELTOID("Deltoid - Right", "Deltoid - Right", "RD"),
        LEFT_THIGH("Thigh - Left", "Thigh - Left", "LT"),
        RIGHT_THIGH("Thigh - Right", "Thigh - Right", "RT"),
        LEFT_ANTERIOR_THIGH("Anterior Thigh - Left", "Ant. Thigh-Lt", "LAT"),
        RIGHT_ANTERIOR_THIGH("Anterior Thigh - Right", "Ant. Thigh-Rt", "RAT"),
        LEFT_LATERAL_THIGH("Lateral Thigh - Left", "Lat. Thigh-Lt", "LLT"),
        RIGHT_LATERAL_THIGH("Lateral Thigh - Right", "Lat. Thigh-Rt", "RLT"),
        LEFT_GLUTEUS_MEDIUS("Gluteus Medius - Left", "Gluteus Med-Lt", "LG"),
        RIGHT_GLUTEUS_MEDIUS("Gluteus Medius - Right", "Gluteus Med-Rt", "RG"),
        ORAL("Oral", "Oral", "PO"),
        NASAL("Nasal", "Nasal", "NS"),
        OTHER("Other", "Other", "OTHER");

        companion object {
            private val map = values().associateBy(SiteValue::abbreviation)

            fun fromString(abbreviation: String) = map[abbreviation] ?: OTHER
        }
    }

    private val commonSites = listOf(
        SiteValue.LEFT_ARM,
        SiteValue.RIGHT_ARM,
        SiteValue.LEFT_DELTOID,
        SiteValue.RIGHT_DELTOID,
        SiteValue.LEFT_THIGH,
        SiteValue.RIGHT_THIGH,
        SiteValue.LEFT_ANTERIOR_THIGH,
        SiteValue.RIGHT_ANTERIOR_THIGH,
        SiteValue.LEFT_LATERAL_THIGH,
        SiteValue.RIGHT_LATERAL_THIGH,
        SiteValue.LEFT_GLUTEUS_MEDIUS,
        SiteValue.RIGHT_GLUTEUS_MEDIUS,
        SiteValue.OTHER
    )

    val siteMap: HashMap<String, List<SiteValue>> = hashMapOf(
        Pair("IM", commonSites),
        Pair("SC", commonSites),
        Pair("ID", commonSites),
        Pair("PO", listOf(SiteValue.ORAL)),
        Pair("NS", listOf(SiteValue.NASAL))
    )
}
