/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineOrderItemBinding
import com.vaxcare.vaxhub.model.VaccineAdapterOrderDto
import java.time.format.DateTimeFormatter
import java.util.Locale.US

class VaccineItemOrderViewHolder(val binding: RvCheckoutVaccineOrderItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(vaccineItemOrder: VaccineAdapterOrderDto) {
        with(vaccineItemOrder.order) {
            binding.checkoutVaccineName.text = shortDescription
            binding.date.text = orderDate.format(formatter)
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("MM/dd/yy 'AT' HH:mma", US)
    }
}
