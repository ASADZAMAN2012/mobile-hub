/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvPayerSearchResultItemBinding
import com.vaxcare.vaxhub.model.Payer

class SelectPayerResultAdapter(private val defaultPayers: MutableList<Payer>) :
    RecyclerView.Adapter<SelectPayerViewHolder>() {
    var keyword: String? = null
    var onItemClicked: ((payer: Payer) -> Unit)? = null
    private val payers: MutableList<Payer> = mutableListOf()
    private val recentPayers = mutableListOf<Payer>()
    private val isSearchMode: Boolean
        get() {
            return keyword?.isNotBlank() == true && keyword?.isNotEmpty() == true
        }

    override fun getItemCount() =
        if (isSearchMode) {
            payers.size + defaultPayers.size
        } else {
            recentPayers.size + defaultPayers.size + payers.size
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectPayerViewHolder {
        return SelectPayerViewHolder(
            RvPayerSearchResultItemBinding.inflate(parent.getInflater(), parent, false)
        )
    }

    private val dividerPosition: Int
        get() = recentPayers.size

    override fun onBindViewHolder(holder: SelectPayerViewHolder, position: Int) {
        if (isSearchMode) {
            val bgResId = if (position == 0 && itemCount > 1) {
                R.drawable.bg_rounded_ripple_corner_top
            } else if (position < itemCount - 1) {
                R.drawable.bg_rounded_ripple_corner_center
            } else if (position == itemCount - 1) {
                R.drawable.bg_rounded_ripple_corner_bottom
            } else {
                R.drawable.bg_rounded_white_ripple_item
            }
            if (position < payers.size) {
                holder.bind(
                    payers[position],
                    null,
                    bgResId,
                    isDivider = false,
                    onItemClicked
                )
            } else {
                holder.bind(
                    defaultPayers[position - payers.size],
                    null,
                    bgResId,
                    isDivider = false,
                    onItemClicked
                )
            }
        } else {
            val topPayerSize = recentPayers.size + defaultPayers.size
            if (position < topPayerSize) {
                val bgResId = when {
                    position == 0 -> {
                        R.drawable.bg_rounded_ripple_corner_top
                    }

                    position < topPayerSize - 1 -> {
                        R.drawable.bg_rounded_ripple_corner_center
                    }

                    else -> {
                        R.drawable.bg_rounded_ripple_corner_bottom
                    }
                }
                val payer = if (position < recentPayers.size) {
                    recentPayers[position]
                } else {
                    defaultPayers[position - recentPayers.size]
                }
                holder.bind(
                    payer = payer,
                    classify = if (position == 0) {
                        holder.itemView.context.getString(
                            R.string.patient_add_recent_payers_label
                        )
                    } else {
                        null
                    },
                    bgResId = bgResId,
                    isDivider = position == dividerPosition && position != 0,
                    onItemClicked = onItemClicked
                )
            } else {
                val bgResId = if (position == topPayerSize && payers.size > 1) {
                    R.drawable.bg_rounded_ripple_corner_top
                } else if (position < itemCount - 1) {
                    R.drawable.bg_rounded_ripple_corner_center
                } else if (position == itemCount - 1) {
                    R.drawable.bg_rounded_ripple_corner_bottom
                } else {
                    R.drawable.bg_rounded_white_ripple_item
                }
                holder.bind(
                    payers[position - topPayerSize],
                    if (position == topPayerSize) {
                        holder.itemView.context.getString(
                            R.string.patient_add_all_payers
                        )
                    } else {
                        null
                    },
                    bgResId,
                    isDivider = false,
                    onItemClicked
                )
            }
        }
    }

    fun addDefaultPayer(payer: Payer) {
        if (defaultPayers.none { it.id == payer.id }) {
            defaultPayers.add(0, payer)
            notifyDataSetChanged()
        }
    }

    fun addRecentItems(items: List<Payer>) {
        recentPayers.clear()
        recentPayers.addAll(items)
        notifyDataSetChanged()
    }

    fun addAllItems(newList: List<Payer>) {
        payers.clear()
        payers.addAll(newList)
        notifyDataSetChanged()
    }
}
