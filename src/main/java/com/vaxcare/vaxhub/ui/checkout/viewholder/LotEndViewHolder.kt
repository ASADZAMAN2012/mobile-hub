/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvLotAddRowEndBinding

class LotEndViewHolder(val binding: RvLotAddRowEndBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(addNewLot: (() -> Unit)? = null, exactMatch: Boolean) {
        with(itemView) {
            if (exactMatch) {
                hide()
            } else {
                show()
                binding.lotRowBtn.setOnClickListener {
                    addNewLot?.invoke()
                }
            }
        }
    }
}
