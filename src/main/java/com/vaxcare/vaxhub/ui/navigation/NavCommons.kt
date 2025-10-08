/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions

/**
 * Commons method used for navigation
 */
interface NavCommons {
    /**
     * Navigation options
     * You may include the global animation effects you want to use
     */
    val options: NavOptions

    /**
     * Get [NavOptions] to pop to
     *
     * @param popToDestinationId the destination id
     * @param inclusive          true if you want to make it inclusive
     */
    fun getOptionsPopTo(popToDestinationId: Int, inclusive: Boolean): NavOptions

    /**
     * Go to a fragment using a [NavDirections] and the common animations options will be added
     *
     * @param fragment the fragment to get the nav graph
     * @param action   the action to navigate
     */
    fun goToFragment(
        fragment: Fragment?,
        action: NavDirections,
        data: Map<String, Any?>? = null
    )

    /**
     * use this variation when to origin is not in the same nav graph as destination
     *
     * @param view   view of origin, used to find
     * @param action the action to navigate
     */
    fun goToFragment(view: View, action: NavDirections)

    /**
     * Go to a fragment using a [NavDirections] and the common animations options will be added
     * additionally pop to direction is going to be added
     *
     * @param fragment           the fragment to get the nav graph
     * @param action             the action to navigate
     * @param inclusive          pop to inclusive by default is True
     * @param popToDestinationId the destination to pop to, if null is provided the navGraph start destination will be used
     */
    fun goToFragmentPopTo(
        fragment: Fragment?,
        action: NavDirections,
        inclusive: Boolean = true,
        popToDestinationId: Int? = null
    )

    /**
     * Go to a fragment using a destination Id and the common animations options will be added
     *
     * @param fragment      the fragment to get the nav graph
     * @param destinationId the destination Id
     * @param bundle        input params
     */
    fun goToFragment(
        fragment: Fragment?,
        destinationId: Int,
        bundle: Bundle? = null
    )

    /**
     * Go to a fragment using a destination Id and the common animations options will be added
     *
     * @param activity      the activity to get the nav graph
     * @param destinationId the destination Id
     */
    fun goToFragment(
        activity: Activity?,
        destinationId: Int,
        bundle: Bundle? = null
    )

    /**
     * Go to a fragment using a [NavDirections] and the common animations options will be added
     * additionally pop to direction is going to be added
     *
     * @param activity           the activity to get the nav graph
     * @param action             the action to navigate
     * @param inclusive          pop to inclusive by default is True
     * @param popToDestinationId the destination to pop to, if null is provided the navGraph start destination will be used
     */
    fun goToFragmentPopTo(
        activity: Activity?,
        action: NavDirections,
        inclusive: Boolean = true,
        popToDestinationId: Int? = null
    )

    /**
     * Go to a fragment using a [NavDirections] and the common animations options will be added
     * additionally pop to direction is going to be added
     *
     * @param activity           the activity to get the nav graph
     * @param destinationId      the destination id to navigate
     * @param inclusive          pop to inclusive by default is True
     * @param popToDestinationId the destination to pop to, if null is provided the navGraph start destination will be used
     */
    fun goToFragmentPopTo(
        activity: Activity?,
        destinationId: Int,
        bundle: Bundle? = null,
        inclusive: Boolean = true,
        popToDestinationId: Int? = null
    )

    /**
     * Go back pops the back stack
     *
     * @param fragment the fragment to get the nav graph
     * @param backData a Map of key, value that will be added to the backstack in the savedStateHandle
     */
    fun goBack(fragment: Fragment?, backData: Map<String, Any?>? = null)

    /**
     * Go back pops the back stack
     *
     * @param fragment      the fragment to get the nav graph
     * @param destinationId the destination fragment id
     * @param backData      a Map of key, value that will be added to the backstack in the savedStateHandle
     * @param inclusive     true or false to pop inclusive
     */
    fun goBackPopTo(
        fragment: Fragment?,
        destinationId: Int,
        backData: Map<String, Any?>? = null,
        inclusive: Boolean = false
    ): Boolean

    /**
     * Go back pops the back stack
     *
     * @param activity      the activity to get the nav graph
     * @param destinationId the destination fragment id
     * @param backData      a Map of key, value that will be added to the backstack in the savedStateHandle
     * @param inclusive     true or false to pop inclusive
     */
    fun goBackPopTo(
        activity: Activity?,
        destinationId: Int,
        backData: Map<String, Any?>? = null,
        inclusive: Boolean = false
    ): Boolean

    /**
     * Deeplink to the provided URI
     *
     * @param fragment the fragment to get the nav graph
     * @param uri      uri for deep link
     * @param options  options to be used in the navigation
     */
    fun deepLinkTo(
        fragment: Fragment?,
        uri: Uri,
        options: NavOptions? = null
    )
}
