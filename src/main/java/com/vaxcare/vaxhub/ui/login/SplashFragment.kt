/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.vaxcare.core.model.enums.UpdateSeverity
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.ui.extension.withAction
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.AndroidBug5497Workaround.Companion.assistActivity
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.nextMidnight
import com.vaxcare.vaxhub.core.extension.popResultValue
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toOrdinalDate
import com.vaxcare.vaxhub.core.extension.toStandardDate
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.core.ui.BottomDialogOptions
import com.vaxcare.vaxhub.core.ui.StickyItemOptions
import com.vaxcare.vaxhub.databinding.FragmentSplashBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.LocationData
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.metric.CheckoutStartMetric
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.service.SessionCleanService
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.ui.login.OutOfDateFragment.Companion.OUT_OF_DATE_KEY
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.SplashDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.SplashViewModel
import com.vaxcare.vaxhub.viewmodel.TimePeriod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var destination: SplashDestination

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var sessionCleanService: SessionCleanService

    override val displayLoadingByDefault: Boolean = true

    private val viewModel: SplashViewModel by viewModels()
    private var isLoginSSO = false
    private var loadingJob: Job? = null

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_splash,
        hasMenu = false,
        hasToolbar = false,
        showControlPanel = true,
        showStatusBarIcons = true
    )

    private val clinics = TempClinics()
    private var hasViewedOutOfDate = false

    /**
     * If the user hits the power button on a fragment using the AndroidBug5497Workaround
     * They will land here when they next open the app, so clean up the workaround
     */
    override fun onResume() {
        super.onResume()
        assistActivity(requireActivity()).stopAssistingActivity()
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.availableUpdate.collectLatest { vaxCareUpdate ->
                binding?.notificationOutOfDateContainer?.isVisible =
                    vaxCareUpdate.stagedUpdateAvailable
            }
        }
    }

    override fun onLoadingStart() {
        super.onLoadingStart()
        binding?.loadingBackground?.show()
    }

    override fun onLoadingStop() {
        super.onLoadingStop()
        binding?.loadingBackground?.hide()
    }

    override fun bindFragment(container: View): FragmentSplashBinding = FragmentSplashBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        hasViewedOutOfDate = popResultValue<Boolean>(OUT_OF_DATE_KEY) ?: false
        viewModel.clearStaleSignaturesActiveUserAndSessionId()
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoadingState -> startLoading()
                is SplashViewModel.SplashState.DataLoaded -> onDataLoaded(
                    location = state.location,
                    clinicList = state.clinics,
                    currentClinicId = state.currentClinicId,
                    parentClinicId = state.parentClinicId,
                    inAppUpdateEnabled = state.inAppUpdateEnabled,
                    timePeriod = state.timePeriod,
                    updateSeverity = state.updateSeverity
                )

                SplashViewModel.SplashState.Error -> onError()
            }
        }

        if (BuildConfig.DEBUG) {
            binding?.splashLogo?.setOnClickListener {
                // force a runtime exception for testing
                throw RuntimeException("Exception thrown for testing crash report")
            }
        }

        refreshData()
    }

    /**
     * Refresh our data. Makes sure current job is cancelled before starting a new job.
     */
    private fun refreshData() {
        loadingJob?.cancel()
        loadingJob = viewModel.loadData()
    }

    private fun switchClinic(newClinic: Clinic) {
        lifecycleScope.launch {
            viewModel.switchClinic(newClinic)
            refreshData()
        }
    }

    /**
     * Populates the BottomDialog choices with temp clinics
     */
    private fun selectTempClinic() {
        val filteredClinics = clinics.tempClinics
            .filter { it.isTemporaryClinic() }
            .toMutableList()
            .apply { sortBy { it.name } }

        val clinicNames = filteredClinics.map { it.tempClinicName() }
        val selectedClinic = clinics.tempClinics.firstOrNull { it.selected }
        createBottomDialogAndShow(clinicNames, filteredClinics, selectedClinic)
    }

    private fun createBottomDialogAndShow(
        clinicNames: List<String>,
        filteredClinics: MutableList<Clinic>,
        selectedClinic: Clinic?
    ) {
        // offset padding to add to the Bottom Dialog to avoid item cut offs
        val offsetSplashFragmentPadding = resources.getDimension(R.dimen.dp_50).toInt()
        val shouldBold = (selectedClinic?.id ?: clinics.parentClinicId) == clinics.parentClinicId
        BottomDialog.newInstance(
            title = resources.getString(R.string.temporary_clinic_title),
            values = clinicNames,
            selectedIndex = filteredClinics.indexOfFirst { it.selected },
            options = BottomDialogOptions(
                stickyItemOpts = StickyItemOptions(
                    itemValue = clinics.parentClinicName,
                    bolded = shouldBold
                ),
                listHeader = resources.getString(R.string.temporary_clinics),
                decoration = VerticalSpaceDecorator(requireContext()),
                controlIconsRes = listOf(R.drawable.ic_sort_alpha, R.drawable.ic_sort_clock),
                extraBottomPadding = offsetSplashFragmentPadding
            )
        ).apply {
            onSelected = { index ->
                val clinic = if (index == -1) {
                    // sticky item (parent clinic) was selected
                    clinics.tempClinics.firstOrNull { it.id == clinics.parentClinicId }
                } else {
                    filteredClinics[index]
                }

                clinic?.let { newClinic ->
                    clinics.tempClinics.find { it.selected }?.selected = false
                    newClinic.selected = true
                    switchClinic(newClinic)
                }
            }
            onControlIconSelected = { onClinicSort(it == 0, filteredClinics, this) }
        }.show(childFragmentManager, "temp clinics")
    }

    private fun Clinic.tempClinicName() = "${name}${startDate?.let { date -> "\n${date.toStandardDate()}" }}"

    /**
     * Callback for control icon selection on bottomDialog
     *
     * @param sortByAlpha flag for sort alphabetically
     * @param filteredClinics list to sort
     * @param dlg reference to bottomDialog to push the items
     */
    private fun onClinicSort(
        sortByAlpha: Boolean,
        filteredClinics: MutableList<Clinic>,
        dlg: BottomDialog
    ) {
        clinics.sortByAlpha = sortByAlpha
        if (sortByAlpha) {
            filteredClinics.sortBy { it.name }
        } else {
            filteredClinics.sortByDescending { it.startDate }
        }

        dlg.setItems(
            newItems = filteredClinics.map {
                "${it.name}${
                    it.startDate?.let { date ->
                        "\n${date.toStandardDate()}"
                    }
                }"
            },
            newIndex = filteredClinics.indexOfFirst { it.selected }
        )
    }

    private fun onDataLoaded(
        location: LocationData?,
        clinicList: List<Clinic>,
        currentClinicId: Long,
        parentClinicId: Long,
        inAppUpdateEnabled: Boolean,
        timePeriod: TimePeriod,
        updateSeverity: UpdateSeverity
    ) {
        if (!hasViewedOutOfDate) {
            when (updateSeverity) {
                UpdateSeverity.NoAction -> Unit
                else -> goToOutOfDate(
                    severity = updateSeverity,
                    partnerName = location?.partnerName ?: "",
                    clinicName = location?.clinicName ?: ""
                )
            }
        }
        binding?.container?.setOnSingleClickListener { goToLogin() }
        refreshClinicList(clinicList, currentClinicId, parentClinicId)
        endLoading()
        if (inAppUpdateEnabled) {
            context?.sendBroadcast(Intent().withAction(Receivers.IN_APP_UPDATE_ACTION))
        }
        location?.activeFeatureFlags?.let { flags ->
            isLoginSSO =
                flags.any { it.featureFlagName == FeatureFlagConstant.FeatureHubLoginUserAndPin.value }
        }

        if (isLoginSSO) {
            sessionCleanService.scheduleSessionClean(context, LocalDate.now().nextMidnight())
        } else {
            sessionCleanService.cancelPendingSession(context = context)
        }

        binding?.partnerName?.text = location?.partnerName ?: ""
        val clinic = clinicList.find { it.id == currentClinicId }
        if (clinic == null) {
            onEmptyClinicData(location)
        } else {
            onClinicDataAvailable(location, clinic, clinicList)
        }

        val resources: SplashResources = when (timePeriod) {
            TimePeriod.Morning -> SplashResources.Morning
            TimePeriod.Afternoon -> SplashResources.Afternoon
            TimePeriod.AfterHours -> SplashResources.AfterHours
        }

        binding?.customMessage?.text = getString(resources.textRes)
        binding?.splashLogo?.setImageResource(resources.logoRes)
        context?.let {
            binding?.container?.setBackgroundColor(ContextCompat.getColor(it, resources.bgColorRes))
        }
        binding?.notificationFab?.setOnSingleClickListener { showUpdatePrompt() }
    }

    private fun goToOutOfDate(
        severity: UpdateSeverity,
        partnerName: String,
        clinicName: String
    ) {
        destination.goToOutOfDate(
            fragment = this@SplashFragment,
            severity = severity,
            partnerName = partnerName,
            clinicName = clinicName
        )
    }

    /**
     * Invalidate the clinics object
     *
     * @param clinicList list of clinics from backend call
     * @param currentClinicId the current selected clinicId
     * @param parentClinicId the parent clinicId
     */
    private fun refreshClinicList(
        clinicList: List<Clinic>,
        currentClinicId: Long,
        parentClinicId: Long
    ) {
        clinicList.find { it.id == currentClinicId }?.selected = true
        clinics.parentClinicName = clinicList.find { it.id == parentClinicId }?.name ?: ""
        clinics.parentClinicId = parentClinicId
        clinics.tempClinics.apply {
            clear()
            addAll(
                clinicList.filter {
                    it.id == parentClinicId || it.isTempDatesBetween()
                }
            )
        }
    }

    private fun onEmptyClinicData(location: LocationData?) {
        // clinics data not yet available
        if (location?.clinicName == location?.partnerName) {
            binding?.clinicName?.invisible()
        } else {
            binding?.clinicName?.show()
            binding?.clinicName?.text = location?.clinicName ?: ""
        }
        binding?.clinicEdit?.hide()
        binding?.clinicDate?.hide()
        binding?.clinicContainer?.setOnSingleClickListener { goToLogin() }
    }

    private fun onClinicDataAvailable(
        location: LocationData?,
        clinic: Clinic,
        clinics: List<Clinic>
    ) {
        val existLegalTemporaryClinics = clinics.any { it.isTempDatesBetween() }
        if (existLegalTemporaryClinics) {
            binding?.clinicName?.show()
            binding?.clinicName?.text = clinic.name
            binding?.clinicEdit?.show()
            binding?.clinicContainer?.setOnSingleClickListener { selectTempClinic() }
        } else {
            if (clinic.name == location?.partnerName) {
                binding?.clinicName?.invisible()
            } else {
                binding?.clinicName?.show()
                binding?.clinicName?.text = clinic.name
            }
            binding?.clinicEdit?.hide()
            binding?.clinicContainer?.setOnSingleClickListener {
                goToLogin()
            }
        }

        if (clinic.isTemporaryClinic() && clinic.startDate != null) {
            binding?.clinicDate?.show()
            binding?.clinicDate?.text = clinic.startDate.toOrdinalDate()
        } else {
            binding?.clinicDate?.hide()
        }
    }

    private fun goToLogin() {
        analytics.saveMetric(
            CheckoutStartMetric(
                networkStatus = networkMonitor.networkStatus.value ?: NetworkStatus.DISCONNECTED
            )
        )

        // pass the appropriate date override
        val selectedDate = clinics.getSelectedClinic()?.let {
            when {
                it.isTemporaryClinic() -> it.startDate
                else -> null
            }
        }
        if (isLoginSSO) {
            destination.goToSSOLogin(this, selectedDate)
        } else {
            destination.goToPinLock(
                fragment = this@SplashFragment,
                pinLockAction = PinLockAction.LOGIN,
                selectedDate = selectedDate
            )
        }
    }

    private fun onError() {
        // Stop loading
        endLoading()
    }

    /**
     * Wrapper object for holding clinics and view metadata
     *
     * @property tempClinics list of all temp clinics (and parent clinic)
     * @property sortByAlpha flag for sorting by name ASC
     * @property parentClinicName name of the parentClinic
     * @property parentClinicId id of the parentClinic
     */
    private data class TempClinics(
        val tempClinics: MutableList<Clinic> = mutableListOf(),
        var sortByAlpha: Boolean = false,
        var parentClinicName: String = "",
        var parentClinicId: Long = 0L
    ) {
        fun getSelectedClinic() = tempClinics.firstOrNull { it.selected }
    }

    private fun showUpdatePrompt() {
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) {
            when (it) {
                ErrorDialogButton.PRIMARY_BUTTON -> updateAppAndSavePromptMetric()
                else -> viewModel.saveUpdatePromptMetric(
                    buttonTitle = getString(R.string.prompt_later),
                    versionName = BuildConfig.VERSION_NAME
                )
            }

            removeResult<ErrorDialogButton>(ErrorDialog.RESULT)
        }

        globalDestinations.goToErrorDialog(
            fragment = this@SplashFragment,
            title = R.string.notification_out_of_date_title,
            body = R.string.prompt_out_of_date_message,
            primaryBtn = R.string.prompt_update_now,
            secondaryBtn = R.string.prompt_later
        )
    }

    private fun updateAppAndSavePromptMetric() {
        with(viewModel) {
            saveUpdatePromptMetric(
                buttonTitle = getString(R.string.prompt_update_now),
                versionName = BuildConfig.VERSION_NAME
            )
            immediateUpdate(activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor.setupSignalStrengthListeners(lifecycle)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        super.onDestinationChanged(controller, destination, arguments)
        viewModel.resetState()
    }
}

sealed class SplashResources(
    @StringRes val textRes: Int,
    @ColorRes val bgColorRes: Int,
    @DrawableRes val logoRes: Int
) {
    object Morning : SplashResources(
        textRes = R.string.splash_text_morning,
        bgColorRes = R.color.primary_coral,
        logoRes = R.drawable.ic_logo_mark_coral
    )

    object Afternoon : SplashResources(
        textRes = R.string.splash_text_afternoon,
        bgColorRes = R.color.primary_magenta,
        logoRes = R.drawable.ic_logo_mark_fuschia
    )

    object AfterHours : SplashResources(
        textRes = R.string.splash_text_after_hours,
        bgColorRes = R.color.purple_secondary,
        logoRes = R.drawable.ic_logo_mark_purple
    )
}
