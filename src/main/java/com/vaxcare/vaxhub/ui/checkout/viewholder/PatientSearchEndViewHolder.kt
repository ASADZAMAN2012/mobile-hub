/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvPatientSearchEndItemBinding

class PatientSearchEndViewHolder(val binding: RvPatientSearchEndItemBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private val dpDisplacementWithResults =
        binding.root.context.resources.getDimension(R.dimen.dp_26).toInt()
    private val dpDisplacementWithoutResults =
        binding.root.context.resources.getDimension(R.dimen.dp_127).toInt()

    fun bind(addNewLot: (() -> Unit)? = null, hasResults: Boolean) {
        with(itemView) {
            val lp: RecyclerView.LayoutParams = layoutParams as RecyclerView.LayoutParams
            val strRes = if (hasResults) {
                R.string.patient_search_not_finding
            } else {
                R.string.patient_search_not_existing
            }
            val topCorrection = if (hasResults) {
                dpDisplacementWithResults
            } else {
                dpDisplacementWithoutResults
            }

            show()
            lp.topMargin = topCorrection
            binding.lotRowText.setText(strRes)
            layoutParams = lp
            binding.lotRowBtn.setOnClickListener {
                addNewLot?.invoke()
            }
        }
    }
}
