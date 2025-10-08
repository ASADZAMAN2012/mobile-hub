/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import java.math.BigDecimal

fun BigDecimal.formatAmount(scale: Int): Double {
    return setScale(scale, BigDecimal.ROUND_HALF_UP).toDouble()
}
