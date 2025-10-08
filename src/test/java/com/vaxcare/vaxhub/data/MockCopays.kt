/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.data

import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import java.math.BigDecimal

object MockCopays {
    val responseZosterOnly = MedDInfo(
        eligible = true,
        copays = listOf(
            ProductCopayInfo(
                antigen = MedDVaccines.ZOSTER,
                copay = BigDecimal.ONE
            )
        )
    )
}
