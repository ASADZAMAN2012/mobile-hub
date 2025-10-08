/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvMedDCheckItemBinding
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDCheckItemViewHolder

class MedDCheckAdapter(
    private val copays: MutableList<ProductCopayInfo> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MedDCheckItemViewHolder(
            RvMedDCheckItemBinding.inflate(parent.getInflater(), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MedDCheckItemViewHolder).bind(copays[position])
    }

    override fun getItemCount() = copays.size
}
