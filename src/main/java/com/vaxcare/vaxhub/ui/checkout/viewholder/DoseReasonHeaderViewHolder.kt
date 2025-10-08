/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.core.view.isInvisible
import com.vaxcare.vaxhub.databinding.RvDoseReasonHeaderBinding
import com.vaxcare.vaxhub.ui.checkout.adapter.BaseDoseReasonViewHolder

class DoseReasonHeaderViewHolder(
    val binding: RvDoseReasonHeaderBinding
) : BaseDoseReasonViewHolder(binding.root) {
    fun bind(titleId: Int, hideSubtitle: Boolean) {
        binding.reviewTitle.setText(titleId)
        binding.reviewSubtitle.isInvisible = hideSubtitle
    }
}
