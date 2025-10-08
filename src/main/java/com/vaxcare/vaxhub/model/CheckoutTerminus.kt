/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import android.os.Parcelable
import com.vaxcare.vaxhub.model.inventory.OrderProductWrapper
import com.vaxcare.vaxhub.model.patient.InvalidInfoWrapper
import kotlinx.parcelize.Parcelize

/**
 * Classes for handling different destinations when navigating towards the CheckoutSummaryFragment.
 *
 * The idea was to wrap these in a an object that holds a collection of CheckoutTerminus but this
 * proved to be a limitation of the library as Java has type erasure. Instead we are explicitly
 * passing these classes in the flow.
 */
interface CheckoutTerminus : Parcelable

@Parcelize
data class DoseReasons(val orderProductWrapper: OrderProductWrapper) : CheckoutTerminus

@Parcelize
data class MissingInfo(val invalidInfoWrapper: InvalidInfoWrapper) : CheckoutTerminus
