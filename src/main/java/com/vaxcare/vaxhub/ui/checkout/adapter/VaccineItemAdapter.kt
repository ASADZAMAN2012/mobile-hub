/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineItemBinding
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineOrderItemBinding
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.VaccineAdapterDto
import com.vaxcare.vaxhub.model.VaccineAdapterOrderDto
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.VaccineWithIssues
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.DeleteActionType
import com.vaxcare.vaxhub.model.enums.RouteCode
import com.vaxcare.vaxhub.model.inventory.Site
import com.vaxcare.vaxhub.model.order.OrderEntity
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemOrderViewHolder
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemProductViewHolder
import timber.log.Timber
import java.util.Locale

class VaccineItemAdapter(
    private val listener: VaccineItemAdapterListener
) : VaccineItemOptionsListener, RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val PRODUCT_TYPE = 1
        private const val ORDER_TYPE = 2
    }

    val vaccineItems: MutableList<VaccineAdapterDto> = mutableListOf()

    override fun getItemCount() = vaccineItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            PRODUCT_TYPE -> VaccineItemProductViewHolder(
                RvCheckoutVaccineItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ),
                this
            )

            else -> VaccineItemOrderViewHolder(
                RvCheckoutVaccineOrderItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

    override fun getItemViewType(position: Int): Int =
        if (vaccineItems[position] is VaccineAdapterProductDto) {
            PRODUCT_TYPE
        } else {
            ORDER_TYPE
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VaccineItemProductViewHolder) {
            val product = vaccineItems[position] as VaccineAdapterProductDto
            holder.bind(product)
        } else if (holder is VaccineItemOrderViewHolder) {
            holder.bind(vaccineItems[position] as VaccineAdapterOrderDto)
        }
    }

    fun updateSite(siteNew: Site.SiteValue, position: Int) {
        if (position in 0 until itemCount) {
            (vaccineItems[position] as? VaccineAdapterProductDto)?.apply {
                site = siteNew
                hasEdit = true
                notifyItemChanged(position)
                listener.onAdapterChanged()
            }
        }
    }

    fun updateRouteAtPosition(routeCode: RouteCode, position: Int) {
        try {
            (vaccineItems[position] as? VaccineAdapterProductDto)?.apply {
                product.routeCode = routeCode
                hasEdit = true
                notifyItemChanged(position)
                listener.onAdapterChanged()
            }
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to update route $routeCode at position $position")
        }
    }

    fun addItem(item: VaccineWithIssues): Int {
        val dto = item.toAdapterDto()
        dto.hasEdit = true

        val indexToRemove = vaccineItems.indexOfFirst {
            it is VaccineAdapterOrderDto && it.order.satisfyingProductIds.contains(dto.salesProductId)
        }
        if (indexToRemove > -1) {
            val order = vaccineItems.removeAt(indexToRemove)
            dto.orderNumber = order.orderNumber
        }
        vaccineItems.add(0, dto)
        sortList(vaccineItems)
        notifyDataSetChanged()
        listener.onAdapterChanged()
        return vaccineItems.indexOf(dto)
    }

    /**
     * Update items using a callback
     *
     * @param mutator callback to mutate the items
     */
    fun updateItems(mutator: (List<VaccineAdapterDto>) -> List<VaccineAdapterDto>) {
        val newList = mutator(vaccineItems.toList())
        with(vaccineItems) {
            clear()
            addAll(newList)
            sortList(this)
        }

        notifyDataSetChanged()
    }

    fun addAllItems(items: List<VaccineAdapterDto>) {
        if (items.isNotEmpty()) {
            vaccineItems.addAll(items)
            sortList(vaccineItems)
            notifyDataSetChanged()
            listener.onAdapterChanged()
        }
    }

    private fun sortList(items: MutableList<VaccineAdapterDto>) {
        items.sortWith(
            compareBy(
                { (it as? VaccineAdapterProductDto)?.isDeleted == true },
                { it.weight },
                { it.product.displayName.lowercase(Locale.US) }
            )
        )
    }

    fun removeByPosition(
        position: Int,
        isDelete: Boolean = false,
        orders: List<OrderEntity>
    ) {
        if (position in 0 until itemCount) {
            if (isDelete) {
                val removedVaccine = vaccineItems.removeAt(position)
                var hasDuplicatedOrder = false
                vaccineItems.filter { it.product.id == removedVaccine.product.id }
                    .forEach {
                        if (it is VaccineAdapterProductDto) {
                            it.restorePaymentModeRevertValuesAfterDeletingDuplicate()
                            hasDuplicatedOrder = it.orderNumber == removedVaccine.orderNumber
                        }
                    }
                if (!hasDuplicatedOrder) {
                    alignOrdersWithRemovedProduct(removedVaccine, orders)
                }
                notifyItemRemoved(position)
                getIndexesOfConflictingItemsWithRemovedProduct(removedVaccine).forEach {
                    notifyItemChanged(it)
                }
                // After this method (notifyItemRemoved), the Item will not be refreshed, which means that the data will not be re-bind
                // so here need to re-bind
                notifyItemRangeChanged(position, itemCount - position)
            } else {
                val vaccine = vaccineItems[position] as VaccineAdapterProductDto

                when (vaccine.doseState) {
                    DoseState.ADMINISTERED -> {
                        vaccine.doseState = DoseState.ADMINISTERED_REMOVED
                    }

                    DoseState.ADDED -> {
                        vaccine.doseState = DoseState.REMOVED
                    }

                    else -> Unit
                }
                vaccine.hasEdit = true
                notifyItemChanged(position)
                getIndexesOfConflictingItemsWithRemovedProduct(vaccine).forEach {
                    notifyItemChanged(it)
                }
            }

            listener.onAdapterChanged()
        }
    }

    fun restoreDoseByPosition(position: Int) {
        if (position in 0 until itemCount) {
            val vaccine = vaccineItems[position] as VaccineAdapterProductDto
            when (vaccine.doseState) {
                DoseState.ADMINISTERED_REMOVED -> {
                    vaccine.doseState = DoseState.ADMINISTERED
                }

                DoseState.REMOVED -> {
                    vaccine.doseState = DoseState.ADDED
                }

                else -> Unit
            }

            vaccine.hasEdit = true
            vaccine.swipeReset = true
            vaccine.vaccineIssues = vaccine.vaccineIssues - setOf(ProductIssue.DuplicateProduct)
            notifyItemChanged(position)
            getIndexesOfConflictingItemsWithAddedProduct(vaccine).forEach {
                notifyItemChanged(it)
            }
            listener.onAdapterChanged()
        }
    }

    fun changeDoseSeries(id: String, doseSeries: Int) {
        val index = vaccineItems.indexOfFirst { it is VaccineAdapterProductDto && it.id == id }
        if (index != -1) {
            (vaccineItems[index] as? VaccineAdapterProductDto)?.let {
                if (it.doseSeries == doseSeries) {
                    return
                }
                it.doseSeries = doseSeries
                it.hasEdit = true
                notifyItemChanged(index)
                listener.onAdapterChanged()
            }
        }
    }

    private fun alignOrdersWithRemovedProduct(removedVaccine: VaccineAdapterDto, orders: List<OrderEntity>) {
        orders.firstOrNull { it.orderNumber == removedVaccine.orderNumber }?.let { order ->
            val dto = VaccineAdapterOrderDto(
                hasEdit = false,
                product = removedVaccine.product,
                salesProductId = removedVaccine.salesProductId,
                doseState = DoseState.ORDERED,
                order = order,
                orderNumber = order.orderNumber
            )

            vaccineItems.add(dto)
            sortList(vaccineItems)
        }
    }

    private fun getIndexesOfConflictingItemsWithAddedProduct(newProduct: VaccineAdapterProductDto): List<Int> {
        val changedItemIds = vaccineItems
            .filterIsInstance<VaccineAdapterProductDto>()
            .filter { it.isSameProduct(newProduct) }
            .map {
                it.vaccineIssues += setOf(ProductIssue.DuplicateProduct)
                it.swipeReset = true
                it.id
            }
        return vaccineItems.mapIndexedNotNull { index, item ->
            if (item.id in changedItemIds) {
                index
            } else {
                null
            }
        }
    }

    private fun getIndexesOfConflictingItemsWithRemovedProduct(removedProduct: VaccineAdapterDto): List<Int> {
        val changedItemIds = vaccineItems
            .filterIsInstance<VaccineAdapterProductDto>()
            .filter { it.isSameProduct(removedProduct) }
            .map {
                it.vaccineIssues -= setOf(ProductIssue.DuplicateProduct)
                it.swipeReset = true
                it.id
            }

        return vaccineItems.mapIndexedNotNull { index, item ->
            if (item.id in changedItemIds) {
                index
            } else {
                null
            }
        }
    }

    fun resetSwipeState(position: Int) {
        (vaccineItems[position] as? VaccineAdapterProductDto)?.swipeReset = true
        notifyItemChanged(position)
    }

    fun itemExistsInCart(itemId: String): Boolean = vaccineItems.any { it.id == itemId }

    override fun onSiteClicked(product: VaccineAdapterProductDto) {
        listener.onSiteClicked(product, vaccineItems.indexOf(product))
    }

    override fun onSwipeActionClicked(product: VaccineAdapterProductDto) {
        listener.onSwipeActionButton(product, vaccineItems.indexOf(product))
    }

    override fun onDeletionAttempt(product: VaccineAdapterProductDto, deleteActionType: DeleteActionType) {
        listener.onDeleteAttempt(product, vaccineItems.indexOf(product), deleteActionType)
    }

    override fun onCheckoutDoseSeries(product: VaccineAdapterProductDto) {
        listener.onCheckoutDoseSeries(product, vaccineItems.indexOf(product))
    }

    override fun onRouteClicked(product: VaccineAdapterProductDto) {
        listener.onRouteClicked(product, vaccineItems.indexOf(product))
    }
}

/**
 * Listener so Viewholder can communicate to the adapter
 */
interface VaccineItemOptionsListener {
    fun onSiteClicked(product: VaccineAdapterProductDto)

    fun onSwipeActionClicked(product: VaccineAdapterProductDto)

    fun onDeletionAttempt(product: VaccineAdapterProductDto, deleteActionType: DeleteActionType)

    fun onCheckoutDoseSeries(product: VaccineAdapterProductDto)

    fun onRouteClicked(product: VaccineAdapterProductDto)
}
