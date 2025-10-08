/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getMainThread
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.core.extension.safeContext
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentCreatePatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CreatePatientFragment : BaseFragment<FragmentCreatePatientBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    private val handler = Handler(Looper.getMainLooper())

    private val args: CreatePatientFragmentArgs by navArgs()

    private val appointmentViewModel: AppointmentViewModel by activityViewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_create_patient,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentCreatePatientBinding =
        FragmentCreatePatientBinding.bind(container)

    private val isNewPatient by lazy { args.patientId > 0 }

    override fun init(view: View, savedInstanceState: Bundle?) {
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.start()
        createPatient(0) { appointmentId ->
            fun completeCreatePatient() {
                viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.Main) {
                    val result = safeContext(Dispatchers.IO) {
                        appointmentViewModel.getUpdatedAppointment(appointmentId)
                    }
                    // Check that the Insurance card is asked
                    val isNormalPayer =
                        appointmentViewModel.appointmentCreation.payer?.isNormalPayer() ?: false
                    val clearBackStack = args.collectPhoneData != null
                    result?.let {
                        val straightToCheckoutOverride = clearBackStack || isNormalPayer
                        if (straightToCheckoutOverride) {
                            globalDestinations.goBackToAppointmentList(this@CreatePatientFragment)
                            globalDestinations.goToCheckout(
                                fragment = this@CreatePatientFragment,
                                appointmentId = it.id,
                                curbsideNewPatient = isNewPatient
                            )
                        } else {
                            globalDestinations.startCheckoutWithCreatePatient(
                                this@CreatePatientFragment,
                                appointment = it,
                                analytics = analytics,
                                data = args.collectPhoneData?.copy(
                                    appointmentId = it.id,
                                    patientId = it.patient.id
                                ),
                                isNewPatient = isNewPatient
                            )
                        }
                    } ?: globalDestinations.goToCheckout(
                        this@CreatePatientFragment,
                        appointmentId = appointmentId,
                        curbsideNewPatient = isNewPatient
                    )
                }
            }

            // All create by existed patients or just med d?
            if (isNewPatient) {
                // Create appointment by existed patient, make sure risk is not null
                fetchAppointmentLoop(appointmentId, System.currentTimeMillis()) {
                    completeCreatePatient()
                }
            } else {
                completeCreatePatient()
            }
        }
    }

    private fun createPatient(count: Int, onCompletion: (appointmentId: Int) -> Unit) {
        val patientId = args.patientId
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.IO) {
            val wrapperResult = if (patientId > 0) {
                appointmentViewModel.createAppointmentWithPatientId(
                    patientId = patientId,
                    providerId = args.providerId,
                    patientCollectData = args.collectPhoneData,
                    count = count
                )
            } else {
                appointmentViewModel.createAppointment(
                    count = count,
                    patientCollectData = args.collectPhoneData
                )
            }

            safeContext(Dispatchers.Main) {
                val appointmentId = wrapperResult.first
                when {
                    appointmentId != null -> {
                        // After creating the appointment, start uploading patient photos in the background
                        // If it fails, we will intercept and save it in OfflineRequestDao, and try again until it succeeds
                        lifecycleScope.launch(Dispatchers.IO) {
                            appointmentViewModel.uploadPatientPhotos(
                                appointmentId.toInt()
                            )

                            withContext(Dispatchers.Main) {
                                onCompletion.invoke(appointmentId.toInt())
                            }
                        }
                    }
                    wrapperResult.second < CREATE_PATIENT_RETRY_TIMES -> {
                        Timber.d("Create patient failed, retrying...")
                        createPatient(wrapperResult.second, onCompletion)
                    }
                    else -> context?.getMainThread {
                        Timber.e("Create patient failed.")
                        context?.makeLongToast(R.string.patient_add_failed_to_create_appointment)
                        globalDestinations.goBack(this@CreatePatientFragment)
                    }
                }
            }
        }
    }

    private fun fetchAppointmentLoop(
        appointmentId: Int,
        startTimeMillis: Long,
        onCompletion: () -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.Main) {
            val result = safeContext(Dispatchers.IO) {
                appointmentViewModel.getUpdatedAppointment(appointmentId)
            }
            when {
                result?.encounterState?.messages?.firstOrNull() != null -> {
                    onCompletion.invoke()
                }
                System.currentTimeMillis() < startTimeMillis + MAX_DELAY_DURATION -> {
                    handler.postDelayed({
                        fetchAppointmentLoop(appointmentId, startTimeMillis, onCompletion)
                    }, RETRY_INTERVAL)
                }
                else -> {
                    onCompletion.invoke()
                }
            }
        }
    }

    companion object {
        private const val CREATE_PATIENT_RETRY_TIMES = 3
        private const val RETRY_INTERVAL = 1000L
        private const val MAX_DELAY_DURATION = 30 * 1000L
    }
}
