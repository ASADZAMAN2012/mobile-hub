/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.work.WorkManager
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getMainThread
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.model.OverlayProperties
import com.vaxcare.vaxhub.core.ui.BaseOverlay
import com.vaxcare.vaxhub.databinding.OverlaySettingUpVaxhubBinding
import com.vaxcare.vaxhub.ui.navigation.AdminDestination
import com.vaxcare.vaxhub.viewmodel.AdminViewModel
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AdminSetupOverlay : BaseOverlay<OverlaySettingUpVaxhubBinding>() {
    override val baseOverlayProperties: OverlayProperties = OverlayProperties()

    override fun canShowConnection(): Boolean = false

    private val adminViewModel: AdminViewModel by activityViewModels()

    @Inject
    lateinit var destination: AdminDestination

    @Inject
    lateinit var hiltWorkManagerListener: HiltWorkManagerListener

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup) =
        OverlaySettingUpVaxhubBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        adminViewModel.hasError = false
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.start()
        setupVaxHubData(0)
    }

    private fun setupVaxHubData(count: Int) {
        adminViewModel.setupVaxHub(count, requireContext()) { result: Boolean, tries: Int ->
            Timber.d("Got $result and $tries")
            when {
                result -> {
                    Timber.d("Setup succeeded")
                    context?.let {
                        OneTimeWorker.buildOneTimeUniqueWorker(
                            wm = WorkManager.getInstance(it),
                            parameters = OneTimeParams.PingJob,
                            listener = hiltWorkManagerListener
                        )

                        it.getMainThread {
                            binding?.textView?.text =
                                getString(R.string.admin_fragment_setup_complete)
                            binding?.loading?.hide()
                            (binding?.loading?.drawable as? AnimatedImageDrawable)?.stop()
                            Handler(Looper.getMainLooper()).postDelayed(
                                {
                                    destination.goBack(this@AdminSetupOverlay)
                                },
                                resources.getInteger(R.integer.admin_fragment_setup_complete_message_delay)
                                    .toLong()
                            )
                        }
                    }
                }

                tries < 3 -> {
                    Timber.d("Setup failed, retrying...")
                    setupVaxHubData(tries)
                }

                else -> {
                    context?.getMainThread {
                        Timber.d("Setup failed")
                        destination.goBack(this@AdminSetupOverlay)
                        adminViewModel.hasError = true
                    }
                }
            }
        }
    }
}
