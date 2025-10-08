/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.adapter

import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.enums.DeleteActionType

/**
 * Callback interface for [VaccineItemAdapter]
 */
interface VaccineItemAdapterListener {
    /**
     * On site clicked
     *
     * @param product product related
     * @param position position in the list
     */
    fun onSiteClicked(product: VaccineAdapterProductDto, position: Int)

    /**
     * Notify back on Adapter Changed
     */
    fun onAdapterChanged()

    /**
     * Notify the swipe action button was tapped
     *
     * @param product product related
     * @param position position in the list
     */
    fun onSwipeActionButton(product: VaccineAdapterProductDto, position: Int)

    /**
     * Notify that the view was full swiped
     *
     * @param product product related
     * @param position position in the list
     * @param deleteActionType action type of the triggering event
     */
    fun onDeleteAttempt(
        product: VaccineAdapterProductDto,
        position: Int,
        deleteActionType: DeleteActionType
    )

    /**
     * Notify that the checkout Dose Series was clicked
     *
     * @param product product related
     * @param position position in the list
     */
    fun onCheckoutDoseSeries(product: VaccineAdapterProductDto, position: Int)

    /**
     * On route clicked
     *
     * @param product product related
     * @param position position in the list
     */
    fun onRouteClicked(product: VaccineAdapterProductDto, position: Int)
}
