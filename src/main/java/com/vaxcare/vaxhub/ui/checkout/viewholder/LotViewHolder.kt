/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.span.FontSpan
import com.vaxcare.vaxhub.databinding.RvLotAddRowBinding
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct

class LotViewHolder(val binding: RvLotAddRowBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        lotNumber: LotNumberWithProduct,
        searchText: String?,
        onItemClicked: ((lotNumber: LotNumberWithProduct) -> Unit)?
    ) {
        with(itemView) {
            // val lotNumber = searchableList[position]
            setOnClickListener {
                onItemClicked?.invoke(lotNumber)
            }
            binding.lotRowLotTv.text = lotNumber.name

            if (searchText != null) { // bold your text here

                val bold = resources.getFont(R.font.graphik_medium)
                val regular = resources.getFont(R.font.graphik_regular)

                val split = binding.lotRowLotTv.text.indexOf(searchText)

                val ssb = SpannableStringBuilder(binding.lotRowLotTv.text)

                if (split != -1) {
                    ssb.setSpan(
                        FontSpan(regular),
                        0,
                        split,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                    ssb.setSpan(
                        FontSpan(bold),
                        split,
                        split + searchText.length,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                    ssb.setSpan(
                        FontSpan(regular),
                        split + searchText.length,
                        binding.lotRowLotTv.text.length,
                        Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                }
                binding.lotRowLotTv.text = ssb
            }
            // set the product name
            binding.lotRowProductTv.text = lotNumber.product.displaySpannable(context)
            // set the product presentation icon
            binding.lotRowVaccineIcon.setImageResource(lotNumber.product.presentation.icon)
        }
    }
}
