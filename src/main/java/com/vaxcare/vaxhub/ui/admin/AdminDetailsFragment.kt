/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.admin

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.LoginMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAdminDetailsBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.admin.AdminEnterIDFragment.Companion.NEW_ID
import com.vaxcare.vaxhub.ui.navigation.AdminDestination
import com.vaxcare.vaxhub.web.WebServer
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject

@AndroidEntryPoint
class AdminDetailsFragment : BaseFragment<FragmentAdminDetailsBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var webServer: WebServer

    @Inject
    lateinit var localStorage: LocalStorage

    @Inject
    lateinit var destination: AdminDestination

    private lateinit var type: AdminInfoType
    private var partnerId: Long = 0L
    private var clinicId: Long = 0L

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_admin_details,
        hasToolbar = false
    )

    override fun bindFragment(container: View) = FragmentAdminDetailsBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        setupUIAndData()
        setupListeners()
    }

    private fun setupUIAndData() {
        partnerId = if (partnerId > 0) partnerId else localStorage.partnerId
        clinicId = if (clinicId > 0) clinicId else localStorage.clinicId
        setPartnerID(
            state = if (partnerId == 0L) State.EMPTY else State.HAS_VALUE,
            partnerId = partnerId.toString()
        )
        setClinicID(
            state = if (clinicId == 0L) State.EMPTY else State.HAS_VALUE,
            clinicId = clinicId.toString()
        )

        binding?.serialNumberValue?.text = localStorage.deviceSerialNumber
    }

    private fun setPartnerID(state: State, partnerId: String? = null) {
        binding?.apply {
            when (state) {
                State.EMPTY -> {
                    btnEnterPartnerId.show()
                    partnerIdValue.hide()
                    partnerIdEdit.hide()
                    partnerIdProgressBar.hide()
                }
                State.LOADING -> {
                    btnEnterPartnerId.hide()
                    partnerIdValue.hide()
                    partnerIdEdit.hide()
                    partnerIdProgressBar.show()
                    partnerIdErrorLabel.hide()
                }
                State.HAS_VALUE -> {
                    partnerId?.let {
                        partnerIdValue.text = partnerId
                        this@AdminDetailsFragment.partnerId = partnerId.toLong()
                    }
                    btnEnterPartnerId.hide()
                    partnerIdValue.show()
                    partnerIdEdit.show()
                    partnerIdProgressBar.hide()
                }
            }
        }
    }

    private fun setClinicID(state: State, clinicId: String? = null) {
        binding?.apply {
            when (state) {
                State.EMPTY -> {
                    btnEnterClinicId.show()
                    clinicIdValue.hide()
                    clinicIdEdit.hide()
                    clinicIdProgressBar.hide()
                }
                State.LOADING -> {
                    btnEnterClinicId.hide()
                    clinicIdValue.hide()
                    clinicIdEdit.hide()
                    clinicIdProgressBar.show()
                    clinicIdErrorLabel.hide()
                }
                State.HAS_VALUE -> {
                    clinicId?.let {
                        clinicIdValue.text = clinicId
                        this@AdminDetailsFragment.clinicId = clinicId.toLong()
                    }
                    btnEnterClinicId.hide()
                    clinicIdValue.show()
                    clinicIdEdit.show()
                    clinicIdProgressBar.hide()
                }
            }
        }
    }

    private fun checkIDs(callback: (Boolean) -> Unit) {
        if (partnerId > 0 && clinicId > 0) {
            viewLifecycleOwner.lifecycleScope.safeLaunch {
                try {
                    val partnerId = binding?.partnerIdValue?.text.toString()
                    val clinicId = binding?.clinicIdValue?.text.toString()
                    setPartnerID(State.LOADING)
                    setClinicID(State.LOADING)
                    val success = webServer.getCheckPartnerAndClinic(partnerId, clinicId)
                    localStorage.tabletId = success.tabletId

                    callback(success.result)

                    Timber.w("success: $success")
                } catch (e: HttpException) {
                    callback(false)
                    Timber.e(e, "Caught HttpException")
                } catch (e: SocketTimeoutException) {
                    callback(false)
                    Timber.e(e, "Caught SocketTimeoutException")
                } catch (e: ConnectException) {
                    callback(false)
                    Timber.e(e, "Caught ConnectException")
                } catch (e: Exception) {
                    callback(false)
                    Timber.e(e, "Unknown Error")
                }
            }
        }
    }

    private fun setupListeners() {
        binding?.apply {
            btnEnterPartnerId.setOnClickListener { navToSetIDScreen(AdminInfoType.PARTNER) }
            partnerIdEdit.setOnClickListener { navToSetIDScreen(AdminInfoType.PARTNER) }
            btnEnterClinicId.setOnClickListener { navToSetIDScreen(AdminInfoType.CLINIC) }
            clinicIdEdit.setOnClickListener { navToSetIDScreen(AdminInfoType.CLINIC) }
            toolbar.onCloseAction = {
                destination.goBackToSplash(this@AdminDetailsFragment)
            }

            getResultLiveData<String>(NEW_ID)?.observe(viewLifecycleOwner) { newID ->
                Timber.d("NewID received: $newID")
                if (newID.isNotEmpty()) {
                    when (type) {
                        AdminInfoType.PARTNER -> {
                            setPartnerID(State.HAS_VALUE, newID)
                        }
                        AdminInfoType.CLINIC -> {
                            setClinicID(State.HAS_VALUE, newID)
                        }
                    }
                    checkIDs { success ->
                        setPartnerID(State.HAS_VALUE)
                        setClinicID(State.HAS_VALUE)
                        if (success) {
                            val partnerId = partnerIdValue.text.toString().toLong()
                            val clinicId = clinicIdValue.text.toString().toLong()

                            // store in partner data
                            localStorage.saveSetup(partnerId, clinicId)

                            // update analytics with current partner data
                            analytics.updatePartnerData(partnerId, clinicId)

                            // sending metric to track partner login or partner switch
                            analytics.saveMetric(
                                LoginMetric()
                            )

                            destination.goFromDetailsToSetup(this@AdminDetailsFragment)
                        } else {
                            partnerIdErrorLabel.show()
                            clinicIdErrorLabel.show()
                        }
                    }
                }
            }
        }
    }

    private fun navToSetIDScreen(type: AdminInfoType) {
        this.type = type

        destination.goFromDetailsToEnterID(this@AdminDetailsFragment, type.value)
    }
}

enum class State {
    EMPTY,
    LOADING,
    HAS_VALUE
}

enum class AdminInfoType(val value: String) {
    PARTNER("Partner"),
    CLINIC("Clinic")
}
