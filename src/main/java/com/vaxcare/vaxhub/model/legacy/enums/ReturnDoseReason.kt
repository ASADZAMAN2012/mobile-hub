/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy.enums

import androidx.annotation.DrawableRes
import com.vaxcare.vaxhub.R

enum class ReturnDoseReason(
    val id: Int,
    val displayName: String,
    @DrawableRes val icon: Int
) {
    EXPIRED(5, "Expired or\nExpiring Soon", R.drawable.legacy_ic_dose_expired),
    DELIVERED_TEMP(2, "Delivered Out of\nTemp Range", R.drawable.legacy_ic_expired),
    FRIDGE_TEMP(6, "Fridge Out of\nTemp Range", R.drawable.legacy_ic_fridge_stock),
    DAMAGED(7, "Damaged in Transit", R.drawable.legacy_ic_broken),
    RECALL(3, "Recall", R.drawable.legacy_ic_recall)
}

enum class LossWasteReason(val id: Int, val displayName: String) {
    BROKEN(0, "Broken"),
    UNCAPPED(1, "Uncapped/Drawn Not Administered")
}
