/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.work.WorkManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.PinInAttempt
import com.vaxcare.core.report.model.PinInStatus
import com.vaxcare.core.report.model.PinningStatus
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPinLockBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.metric.CheckoutPinMetric
import com.vaxcare.vaxhub.ui.Main
import com.vaxcare.vaxhub.ui.navigation.PinLockDestination
import com.vaxcare.vaxhub.viewmodel.PinLockViewModel
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class PinLockFragment : BaseFragment<FragmentPinLockBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: PinLockDestination

    private val pinLockViewModel: PinLockViewModel by viewModels()
    private val args: PinLockFragmentArgs by navArgs()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_pin_lock,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = true,
        showStatusBarIcons = false
    )

    @Inject
    lateinit var hiltWorkManagerListener: HiltWorkManagerListener

    override fun canShowConnection(): Boolean = false

    /**
     *  The last network status, so that we can determine what message to show for reconnection
     */
    private val lastNetworkStatus: NetworkStatus
        get() = (activity as? Main)?.lastNetworkStatus?.networkStatus ?: NetworkStatus.DISCONNECTED

    private var timer: CountDownTimer? = null

    override fun bindFragment(container: View): FragmentPinLockBinding = FragmentPinLockBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.lockKeypad?.onCompletion = { pin ->
            analytics.saveMetric(
                PinInAttempt()
            )

            pinLockViewModel.attemptPinIn(pin)
        }

        binding?.lockKeypad?.onBack = {
            destination.goBackToSplash(this@PinLockFragment)
        }

        pinLockViewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PinLockViewModel.PinLockState.PinSuccess -> {
                    handleSuccessPinIn(
                        user = state.user,
                        pin = state.pin,
                        shouldUploadLogs = state.shouldUploadLogs
                    )
                }

                is PinLockViewModel.PinLockState.PinFailed -> handleFailedPinIn(state.pin)
            }
        }
    }

    private fun handleSuccessPinIn(
        user: User,
        pin: String,
        shouldUploadLogs: Boolean
    ) {
        context?.let {
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = WorkManager.getInstance(it),
                parameters = OneTimeParams.PingJob,
                listener = hiltWorkManagerListener
            )
        }

        binding?.lockKeypad?.hideError()

        analytics.saveMetric(
            PinInStatus(
                pinningStatus = PinningStatus.SUCCESS,
                pinUsed = pin,
                username = user.userName
            ),
            CheckoutPinMetric(true)
        )

        if (shouldUploadLogs) {
            uploadLogs()
        }

        when (args.pinLockAction) {
            PinLockAction.LOGIN -> {
                analytics.updateUserData(user.userId, user.userName.orEmpty(), user.fullName)
                destination.goToAppointmentList(this@PinLockFragment, args.appointmentListDate)
            }
        }
    }

    private fun uploadLogs() {
        context?.let {
            OneTimeWorker.buildOneTimeUniqueWorker(
                WorkManager.getInstance(it),
                OneTimeParams.DiagnosticJob
            )
        }
    }

    private fun handleFailedPinIn(pin: String) {
        analytics.saveMetric(
            PinInStatus(PinningStatus.FAIL, pin),
            CheckoutPinMetric(false)
        )

        // On pin failure, attempt to re-sync users
        if (pinLockViewModel.needUsersSynced() && lastNetworkStatus == NetworkStatus.CONNECTED) {
            binding?.lockKeypad?.showUserSync()
            timer =
                object : CountDownTimer(FIVE_SECONDS_MILLIS, ONE_SECOND_MILLIS) {
                    override fun onTick(p0: Long) {}

                    override fun onFinish() {
                        if (!pinLockViewModel.needUsersSynced() && isDetached.not()) {
                            binding?.lockKeypad?.hideUserSync()
                        }
                    }
                }
            timer?.start()
            pinLockViewModel.updateAllUsers()
        }
        binding?.lockKeypad?.showError()
    }

    override fun onPause() {
        timer?.cancel()
        timer = null

        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        pinLockViewModel.resetState()
    }

    companion object {
        private val FIVE_SECONDS_MILLIS = TimeUnit.SECONDS.toMillis(5)
        private val ONE_SECOND_MILLIS = TimeUnit.SECONDS.toMillis(1)
    }
}
