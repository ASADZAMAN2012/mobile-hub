/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.core.model.enums.InventorySource
import com.vaxcare.vaxhub.model.FeatureFlag
import com.vaxcare.vaxhub.model.LocationData

object MockLocations {
    val emptyLocationDataNoFF = LocationData(
        clinicId = 1,
        partnerId = 1,
        partnerName = "",
        clinicNumber = "",
        clinicName = "",
        address = "",
        city = "",
        state = "",
        zipCode = "",
        primaryPhone = "",
        contactId = 1,
        parentClinicId = 1,
        inventorySources = listOf(InventorySource.PRIVATE),
        activeFeatureFlags = listOf()
    )

    val locationDataWithVfcAndPublicStockFF = LocationData(
        clinicId = 1,
        partnerId = 1,
        partnerName = "",
        clinicNumber = "",
        clinicName = "",
        address = "",
        city = "",
        state = "",
        zipCode = "",
        primaryPhone = "",
        contactId = 1,
        parentClinicId = 1,
        inventorySources = listOf(InventorySource.PRIVATE, InventorySource.VFC),
        activeFeatureFlags = listOf(FeatureFlag(1, "PublicStockPilot"))
    )

    val locationDataWithVfcAndNoFF = LocationData(
        clinicId = 1,
        partnerId = 1,
        partnerName = "",
        clinicNumber = "",
        clinicName = "",
        address = "",
        city = "",
        state = "",
        zipCode = "",
        primaryPhone = "",
        contactId = 1,
        parentClinicId = 1,
        inventorySources = listOf(InventorySource.PRIVATE, InventorySource.VFC),
        activeFeatureFlags = listOf()
    )

    val locationDataWithNoStocksAndPublicStockFF = LocationData(
        clinicId = 1,
        partnerId = 1,
        partnerName = "",
        clinicNumber = "",
        clinicName = "",
        address = "",
        city = "",
        state = "",
        zipCode = "",
        primaryPhone = "",
        contactId = 1,
        parentClinicId = 1,
        inventorySources = listOf(InventorySource.PRIVATE),
        activeFeatureFlags = listOf(FeatureFlag(1, "PublicStockPilot"))
    )
}
