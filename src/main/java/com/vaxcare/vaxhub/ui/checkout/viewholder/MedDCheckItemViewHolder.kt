/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.databinding.RvMedDCheckItemBinding
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.checkout.getDisplaySpannable
import java.math.RoundingMode

class MedDCheckItemViewHolder(val binding: RvMedDCheckItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(copay: ProductCopayInfo) {
        with(itemView) {
            binding.medDVaccineName.text = copay.getDisplaySpannable(context)
            copay.notFoundMessage?.let { resId -> binding.medDVaccinePrice.setText(resId) } ?: run {
                val copayValue = copay.copay.setScale(2, RoundingMode.UP).toDouble()
                binding.medDVaccinePrice.text =
                    context.getString(R.string.med_d_check_copay, copayValue)
            }
        }
    }
}
