/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.model.inventory.OrderDose

interface DoseReasonListener {
    fun onItemClick(item: OrderDose)

    fun onRefresh(finished: Boolean)
}

abstract class BaseDoseReasonViewHolder(view: View) : RecyclerView.ViewHolder(view)
