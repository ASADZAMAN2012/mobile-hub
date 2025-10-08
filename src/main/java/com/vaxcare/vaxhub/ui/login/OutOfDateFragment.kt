/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentOutOfDateBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.metric.OutOfDateScreenOptionSelectedMetric
import com.vaxcare.vaxhub.ui.navigation.BackNavigationHandler
import com.vaxcare.vaxhub.ui.navigation.OutOfDateDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OutOfDateFragment : BaseFragment<FragmentOutOfDateBinding>(), BackNavigationHandler {
    companion object {
        const val OUT_OF_DATE_KEY = "OUT_OF_DATE_KEY"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: OutOfDateDestination

    private val args by navArgs<OutOfDateFragmentArgs>()

    private val severity: UpdateSeverity
        get() = args.severity

    override fun handleBack(): Boolean = true

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_out_of_date,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = true,
        showStatusBarIcons = true
    )

    override fun bindFragment(container: View): FragmentOutOfDateBinding = FragmentOutOfDateBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            partnerName.text = args.partnerName
            clinicName.text = args.clinicName
            when (severity) {
                UpdateSeverity.Blocker -> {
                    title.setText(R.string.device_out_of_date_title_blocker)
                    message.setText(R.string.device_out_of_date_message_blocker)
                    supportMessage.show()
                    buttonLater.hide()
                }

                else -> {
                    title.setText(R.string.device_out_of_date_title_warning)
                    message.setText(R.string.device_out_of_date_message_warning)
                    supportMessage.hide()
                    buttonLater.show()
                }
            }
            buttonUpdateNow.setOnSingleClickListener {
                analytics.saveMetric(
                    OutOfDateScreenOptionSelectedMetric(
                        severity = severity,
                        currentVersionCode = BuildConfig.VERSION_CODE,
                        buttonClicked = getString(R.string.prompt_update_now)
                    )
                )
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${context?.packageName}")
                    )
                )
            }
            buttonLater.setOnSingleClickListener {
                analytics.saveMetric(
                    OutOfDateScreenOptionSelectedMetric(
                        severity = severity,
                        currentVersionCode = BuildConfig.VERSION_CODE,
                        buttonClicked = getString(R.string.prompt_later)
                    )
                )
                destination.goBackToSplash(this@OutOfDateFragment, OUT_OF_DATE_KEY)
            }
        }
    }
}
