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
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.vaxcare.vaxhub.R
import timber.log.Timber

class NavCommonsImpl(private val navHostId: Int) : NavCommons {
    override val options = NavOptions.Builder()
        .setEnterAnim(R.anim.fadein)
        .setExitAnim(R.anim.fadeout)
        .build()

    override fun getOptionsPopTo(popToDestinationId: Int, inclusive: Boolean) =
        NavOptions.Builder()
            .setPopUpTo(popToDestinationId, inclusive)
            .setEnterAnim(R.anim.fadein)
            .setExitAnim(R.anim.fadeout)
            .build()

    override fun goToFragment(
        fragment: Fragment?,
        action: NavDirections,
        data: Map<String, Any?>?
    ) {
        try {
            fragment?.findNavController()?.apply {
                navigate(action, options)
                data?.entries?.forEach { entry ->
                    currentBackStackEntry?.savedStateHandle?.set(entry.key, entry.value)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragment(view: View, action: NavDirections) {
        try {
            Navigation.findNavController(view).navigate(action)
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragment(
        fragment: Fragment?,
        destinationId: Int,
        bundle: Bundle?
    ) {
        try {
            fragment?.findNavController()?.navigate(destinationId, bundle, options)
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragmentPopTo(
        fragment: Fragment?,
        action: NavDirections,
        inclusive: Boolean,
        popToDestinationId: Int?
    ) {
        try {
            fragment?.findNavController()?.let {
                val popTo = popToDestinationId ?: it.graph.startDestinationId
                it.navigate(action, getOptionsPopTo(popTo, inclusive))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragment(
        activity: Activity?,
        destinationId: Int,
        bundle: Bundle?
    ) {
        try {
            activity?.findNavController(navHostId)
                ?.navigate(destinationId, bundle, options)
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragmentPopTo(
        activity: Activity?,
        action: NavDirections,
        inclusive: Boolean,
        popToDestinationId: Int?
    ) {
        try {
            activity?.findNavController(navHostId)?.let {
                val popTo = popToDestinationId ?: it.graph.startDestinationId
                it.navigate(action, getOptionsPopTo(popTo, inclusive))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goToFragmentPopTo(
        activity: Activity?,
        destinationId: Int,
        bundle: Bundle?,
        inclusive: Boolean,
        popToDestinationId: Int?
    ) {
        try {
            activity?.findNavController(navHostId)?.let {
                val popTo = popToDestinationId ?: it.graph.startDestinationId

                it.navigate(destinationId, bundle, getOptionsPopTo(popTo, inclusive))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goBack(fragment: Fragment?, backData: Map<String, Any?>?) {
        try {
            fragment?.findNavController()?.apply {
                popBackStack()

                backData?.entries?.forEach { entry ->
                    currentBackStackEntry?.savedStateHandle?.set(entry.key, entry.value)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }

    override fun goBackPopTo(
        fragment: Fragment?,
        destinationId: Int,
        backData: Map<String, Any?>?,
        inclusive: Boolean
    ): Boolean =
        try {
            var value = false
            fragment?.findNavController()?.apply {
                value = popBackStack(destinationId, inclusive)

                backData?.entries?.forEach { entry ->
                    currentBackStackEntry?.savedStateHandle?.set(entry.key, entry.value)
                }
            }
            value
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
            false
        }

    override fun goBackPopTo(
        activity: Activity?,
        destinationId: Int,
        backData: Map<String, Any?>?,
        inclusive: Boolean
    ): Boolean =
        try {
            var value = false

            activity?.findNavController(navHostId)?.apply {
                value = popBackStack(destinationId, inclusive)

                backData?.entries?.forEach { entry ->
                    currentBackStackEntry?.savedStateHandle?.set(entry.key, entry.value)
                }
            }

            value
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
            false
        }

    override fun deepLinkTo(
        fragment: Fragment?,
        uri: Uri,
        options: NavOptions?
    ) {
        try {
            fragment?.findNavController()?.navigate(uri, options)
        } catch (e: Exception) {
            Timber.e(e, "Exception Caught")
        }
    }
}
