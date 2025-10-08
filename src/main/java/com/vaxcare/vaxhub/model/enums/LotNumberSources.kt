/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

sealed class LotNumberSources(val id: Int) {
    object Shipments : LotNumberSources(1)

    object VaxHubScan : LotNumberSources(2)

    object ManualEntry : LotNumberSources(3)
}
