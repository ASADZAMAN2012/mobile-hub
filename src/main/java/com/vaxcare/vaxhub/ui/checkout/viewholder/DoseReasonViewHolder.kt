/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import com.vaxcare.core.ui.extension.underlined
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.databinding.RvDoseReasonBinding
import com.vaxcare.vaxhub.model.inventory.OrderDose
import com.vaxcare.vaxhub.ui.checkout.adapter.BaseDoseReasonViewHolder
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseReasonListener

class DoseReasonViewHolder(
    val binding: RvDoseReasonBinding
) : BaseDoseReasonViewHolder(binding.root) {
    fun bind(item: OrderDose, listener: DoseReasonListener) {
        binding.vaccineIcon.setImageResource(item.iconResId)
        binding.vaccineProductName.text = item.getDisplayName(itemView.context)
        item.selectedReason?.let { reason ->
            with(binding.displayReasonLabel) {
                setBackgroundResource(R.drawable.bg_rounded_corner_listblue_bottom)
                setText(reason.displayValue)
                underlined()
            }
        } ?: run {
            with(binding.displayReasonLabel) {
                setBackgroundResource(R.drawable.bg_rounded_corner_lightest_gray_bottom)
                setText(R.string.orders_review_set_reason)
                underlined()
            }
        }
        binding.root.setOnSingleClickListener { listener.onItemClick(item) }
    }
}
