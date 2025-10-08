/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvLotAddRowBinding
import com.vaxcare.vaxhub.databinding.RvLotAddRowEndBinding
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.ui.checkout.viewholder.LotEndViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.LotViewHolder

class LotNumberSearchAdapter(
    private val originalList: MutableList<LotNumberWithProduct>,
    private val productId: Int
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    private var searchText: String? = null

    private val searchableList: MutableList<LotNumberWithProduct> = mutableListOf()

    private var exactMatch = false

    var onNothingFound: (() -> Unit)? = null
    var onItemClicked: ((lotNumber: LotNumberWithProduct) -> Unit)? = null
    var addLotClicked: (() -> Unit)? = null

    fun setLotLookupList(newList: MutableList<LotNumberWithProduct>) {
        originalList.clear()
        originalList.addAll(newList)
        searchText?.let {
            filter.filter(it)
        }
    }

    /**
     * Searches a specific item in the list and updates adapter.
     * if the search returns empty then onNothingFound callback is invoked if provided which can be used to update UI
     * @param s the search query or text. It can be null.
     * @param onNothingFound a method-body to invoke when search returns nothing. It can be null.
     */
    fun search(s: String?, onNothingFound: (() -> Unit)? = null) {
        this.searchText = s
        this.onNothingFound = onNothingFound
        filter.filter(s)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            LotViewHolder(
                RvLotAddRowBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        } else {
            LotEndViewHolder(
                RvLotAddRowEndBinding.inflate(
                    parent.getInflater(),
                    parent,
                    false
                )
            )
        }
    }

    override fun getItemCount(): Int {
        // +1 because we are adding the button to the bottom
        return searchableList.count() + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LotViewHolder) {
            if (position < searchableList.size) {
                val lotNumber = searchableList[position]
                holder.bind(lotNumber, searchText, onItemClicked)
            }
        } else if (holder is LotEndViewHolder) {
            holder.bind(addLotClicked, exactMatch)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == searchableList.count()) 1 else 0
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val filterList = if (constraint.isNullOrBlank()) {
                    originalList
                } else {
                    originalList.filter {
                        (productId == 0 || it.productId == productId) &&
                            it.name.contains(constraint)
                    }
                }
                return filterResults.also {
                    it.values = filterList
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                @Suppress("UNCHECKED_CAST")
                val filterList = results?.values as? MutableList<LotNumberWithProduct>

                if (filterList.isNullOrEmpty()) {
                    onNothingFound?.invoke()
                }
                exactMatch = filterList?.any { it.name == constraint } ?: false

                searchableList.clear()
                searchableList.addAll(filterList ?: emptyList())
                notifyDataSetChanged()
            }
        }
    }
}
