/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

sealed class TestSites(
    val displayName: String,
    val truncatedName: String,
    val abbreviation: String
) {
    object LeftArm : TestSites("Arm - Left", "Arm - Left", "LA")

    object RightDeltoid : TestSites("Deltoid - Right", "Deltoid - Right", "RD")

    object Oral : TestSites("Oral", "Oral", "PO")

    object Nasal : TestSites("Nasal", "Nasal", "NS")

    object RightArm : TestSites("Arm - Right", "Arm - Right", "RA")

    object NoSelect : TestSites("Set Site", "Set Site", "Set Site")
}
