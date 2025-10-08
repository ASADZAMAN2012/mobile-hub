/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvPayerSearchResultItemBinding
import com.vaxcare.vaxhub.model.Payer

class SelectPayerViewHolder(val binding: RvPayerSearchResultItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        payer: Payer,
        classify: String?,
        bgResId: Int,
        isDivider: Boolean,
        onItemClicked: ((payer: Payer) -> Unit)?
    ) {
        with(itemView) {
            binding.divider.isVisible = isDivider
            setPayerName(payer)
            setOnClickListener {
                onItemClicked?.invoke(payer)
            }

            if (classify != null) {
                binding.tvClassify.text = classify
                binding.tvClassify.show()
            } else {
                binding.tvClassify.hide()
            }

            binding.payerName.setBackgroundResource(bgResId)
        }
    }

    private fun View.setPayerName(payer: Payer) {
        binding.payerName.text = payer.getName(context)
    }
}
