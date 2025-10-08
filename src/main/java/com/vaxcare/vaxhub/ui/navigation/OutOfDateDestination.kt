/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.navigation

import androidx.fragment.app.Fragment
import com.vaxcare.vaxhub.R

interface OutOfDateDestination {
    fun goBackToSplash(fragment: Fragment?, key: String)
}

class OutOfDateDestinationImpl(private val navCommons: NavCommons) : OutOfDateDestination {
    override fun goBackToSplash(fragment: Fragment?, key: String) {
        navCommons.goBackPopTo(
            fragment = fragment,
            destinationId = R.id.splashFragment,
            backData = mapOf(key to true)
        )
    }
}
