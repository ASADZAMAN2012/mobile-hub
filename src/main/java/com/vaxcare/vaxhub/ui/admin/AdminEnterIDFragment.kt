/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAdminEnterIdBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.AdminDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdminEnterIDFragment : BaseFragment<FragmentAdminEnterIdBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: AdminDestination

    private val args: AdminEnterIDFragmentArgs by navArgs()

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_admin_enter_id,
        hasToolbar = false
    )

    override fun bindFragment(container: View) = FragmentAdminEnterIdBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        val type = args.type as String
        binding?.toolbar?.setTitle(getString(R.string.admin_fragment_enter_new_x_id, type))

        binding?.keypad?.onSuccessListener = { newID ->
            destination.goBack(this@AdminEnterIDFragment, mapOf(NEW_ID to newID))
        }
        val resId = when (type) {
            AdminInfoType.PARTNER.value -> R.drawable.ic_enter_selector
            AdminInfoType.CLINIC.value -> R.drawable.ic_check_selector
            else -> R.drawable.ic_check_selector
        }
        binding?.keypad?.setEnterButton(resId)
    }

    companion object {
        const val NEW_ID = "newID"
    }
}
