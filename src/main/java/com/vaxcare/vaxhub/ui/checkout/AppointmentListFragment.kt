/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.RelativeDoS.Companion.getRelativeDoS
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.isToday
import com.vaxcare.vaxhub.core.extension.popResultValue
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toOrdinalDate
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.core.view.calendar.OnDateSelectedListener
import com.vaxcare.vaxhub.databinding.FragmentAppointmentListBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.metric.CheckoutAppointmentSelectedMetric
import com.vaxcare.vaxhub.ui.checkout.adapter.AppointmentListFlags
import com.vaxcare.vaxhub.ui.checkout.adapter.AppointmentListItemAdapter
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.ui.checkout.dialog.ProcessingAppointmentDialog
import com.vaxcare.vaxhub.ui.checkout.viewholder.AppointmentClickListener
import com.vaxcare.vaxhub.ui.navigation.AppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel.AppointmentListState.AppointmentsLoaded
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel.AppointmentListState.NavigateToCheckoutPatient
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel.AppointmentListState.NavigateToDoBCapture
import com.vaxcare.vaxhub.viewmodel.AppointmentListViewModel.AppointmentListState.SyncError
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class AppointmentListFragment :
    BaseFragment<FragmentAppointmentListBinding>(),
    OnDateSelectedListener,
    AppointmentClickListener {
    companion object {
        private const val DELAY_SHORT = 200L
        const val APPOINTMENT_DATE = "appointmentDate"
    }

    private val screenTitle = "Schedule"
    private val viewModel: AppointmentListViewModel by viewModels()
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: AppointmentDestination

    private val appointmentAdapter: AppointmentListItemAdapter by lazy {
        AppointmentListItemAdapter(this)
    }

    private val appointmentListDecoration: AppointmentListDecoration by lazy {
        AppointmentListDecoration(requireContext())
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(
            recyclerView: RecyclerView,
            dx: Int,
            dy: Int
        ) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
            lastPosition = layoutManager?.findFirstCompletelyVisibleItemPosition() ?: 0
        }
    }

    private var needScrollToCurrentTimePosition: Boolean = true

    private var lastPosition = -1
    private var lastSelectedDate = LocalDate.now()

    private var patientSchedule = 0
    private var isAddAppt3 = false

    private var clinic: Clinic? = null

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_appointment_list,
        hasToolbar = false,
        showControlPanel = true
    )

    override fun bindFragment(container: View) = FragmentAppointmentListBinding.bind(container)

    override fun handleBack(): Boolean {
        globalDestinations.goBackToSplash(this@AppointmentListFragment)
        return true
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        appointmentViewModel.clearCurrentCheckout()

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is AppointmentsLoaded -> setupUI(
                    it.appointmentList,
                    it.clinic,
                    it.patientSchedule,
                    it.errorSyncing,
                    it.isAddAppt3
                )

                is SyncError -> errorLoading()

                is LoadingState -> showLoading()

                is NavigateToCheckoutPatient -> {
                    globalDestinations.goToCheckout(
                        fragment = this,
                        appointmentId = it.appointmentId,
                        isForceRiskFree = false,
                        isLocallyCreated = false
                    )
                    viewModel.resetState()
                }

                is NavigateToDoBCapture -> {
                    globalDestinations.goToDoBCollection(
                        fragment = this,
                        appointmentId = it.appointmentId,
                        patientId = it.patientId
                    )
                    viewModel.resetState()
                }

                else -> Unit
            }
        }

        // Check if the selected date needs to be reset
        checkDateOfScheduleIfNeedReset()

        binding?.topBar?.onCloseAction = { handleBack() }

        binding?.topBar?.onRightIcon1Click = {
            val bottomDialog = BottomDialog.newInstance(
                resources.getString(R.string.select_patient_schedule),
                resources.getStringArray(R.array.array_patient_schedule).toList(),
                patientSchedule
            )
            bottomDialog.onSelected = { index ->
                if (patientSchedule != index) {
                    patientSchedule = index
                    viewModel.updatePatientSchedule(patientSchedule, lastSelectedDate)
                }
            }
            bottomDialog.onDismissed = {
                binding?.topBar?.highlightRightIcon1(R.color.black)
            }
            binding?.topBar?.highlightRightIcon1(R.color.purple_secondary)
            bottomDialog.show(
                childFragmentManager,
                "patientScheduleBottomDialog"
            )
        }

        binding?.topBar?.onRightIcon2Click = {
            updateAppointmentDate(lastSelectedDate, true)
        }

        binding?.recyclerView?.apply {
            layoutManager = AppointmentLayoutManager(context)
            adapter = appointmentAdapter
            appointmentAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addItemDecoration(appointmentListDecoration)
        }

        setupCalendar()

        needScrollToCurrentTimePosition = true

        viewModel.getFeatureFlags().observe(viewLifecycleOwner) { flags ->
            val isRprd =
                flags.any { it.featureFlagName == FeatureFlagConstant.RightPatientRightDose.value }
            val isPublicStock =
                flags.any { it.featureFlagName == FeatureFlagConstant.FeaturePublicStockPilot.value }

            appointmentAdapter.flags =
                AppointmentListFlags(
                    isRprd,
                    isPublicStock
                )
        }

        binding?.calendarItemContainer?.setOnSingleClickListener { binding?.calendarContainer?.show() }
        binding?.calendarValue?.text = lastSelectedDate.dayOfMonth.toString()

        binding?.fabLookup?.setOnClickListener {
            if (isAddAppt3) {
                destination.goToAppointmentSearch(
                    fragment = this@AppointmentListFragment,
                    appointmentListDate = lastSelectedDate
                )
            } else {
                globalDestinations.goToCurbsidePatientSearch(this@AppointmentListFragment)
            }
        }

        binding?.fabAdd?.setOnClickListener {
            globalDestinations.goToAddAppointmentOrCreatePatient(this@AppointmentListFragment)
        }
    }

    override fun onDestroyView() {
        binding?.recyclerView?.clearOnScrollListeners()
        super.onDestroyView()
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        super.onDestinationChanged(controller, destination, arguments)
        val previousId = controller.previousBackStackEntry?.destination?.id
        if (previousId != R.id.pinLockFragment) {
            viewModel.resetState()
        }
    }

    override fun onDateSelected(dates: List<LocalDate>) {
        val date = dates[0]
        binding?.calendarContainer?.hide()
        val currentDate = lastSelectedDate
        if (currentDate?.dayOfYear == date.dayOfYear && currentDate.year == date.year) {
            return
        }

        lastPosition = -1
        lastSelectedDate = date
        updateAppointmentDate(date, false)
    }

    override fun onItemClicked(appointment: Appointment, flags: AppointmentListFlags) {
        analytics.saveMetric(
            CheckoutAppointmentSelectedMetric(
                visitId = appointment.id,
                dateOfService = appointment.appointmentTime,
                relativeDoS = getRelativeDoS(appointment.appointmentTime, appointment.checkedOut),
                stock = appointment.vaccineSupply,
                risk = appointment.encounterState?.vaccineMessage?.status,
                ormsPresented = appointment.orders.joinToString(separator = ",") { it.shortDescription }
            )
        )

        val tempClinic = clinic?.isTemporaryClinic() == true && clinic?.startDate != null
        when {
            appointment.isProcessing == true -> {
                addProcessingAppointmentCallback()
                destination.goToProcessingAppointmentDialog(this@AppointmentListFragment)
            }

            // if we are checked out, we still want to go to checkout Summary regardless of temp
            appointment.checkedOut -> {
                when {
                    appointment.isEditable == false ->
                        globalDestinations.goToCheckoutSummary(this, appointment.id)

                    tempClinic -> navigateForTempClinic(appointment)
                    else -> globalDestinations.goToCheckout(this, appointment.id)
                }
            }

            tempClinic -> navigateForTempClinic(appointment)

            else -> viewModel.onUncheckedOutAppointmentSelected(appointment)
        }
    }

    /**
     * Check appointment date. It must fall within 90 days in the past.
     *
     * @param appointment appointment for checkout
     */
    private fun navigateForTempClinic(appointment: Appointment) {
        val date = appointment.appointmentTime.toLocalDate()
        when {
            date?.isBefore(LocalDate.now().minusDays(90)) == true ->
                destination.goToTemporaryClinicDialog(
                    this@AppointmentListFragment,
                    resources.getString(R.string.temporary_clinic_dialog_header_before)
                )

            else -> viewModel.onUncheckedOutAppointmentSelected(appointment)
        }
    }

    private fun setupUI(
        appointments: List<Appointment>,
        clinic: Clinic?,
        spPatientSchedule: Int,
        syncError: Boolean,
        isAddAppt3: Boolean
    ) {
        this.clinic = clinic
        this.patientSchedule = spPatientSchedule
        this.isAddAppt3 = isAddAppt3

        if (syncError) {
            errorLoading()
        }

        debounceLoadAppointment(appointments)
        scroll()

        binding?.dateTime?.text = lastSelectedDate.atStartOfDay().toOrdinalDate(" ")

        binding?.fabAdd?.isGone =
            !isAddAppt3 || lastSelectedDate.atStartOfDay() != LocalDate.now().atStartOfDay()
    }

    private fun errorLoading() {
        hideLoading(true)

        viewModel.resetState()

        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) {
            when (it) {
                ErrorDialogButton.PRIMARY_BUTTON -> {
                    showLoading()
                    updateAppointmentDate(
                        appointmentDate = lastSelectedDate,
                        forceRefresh = true
                    )
                }

                else -> Unit
            }
        }

        globalDestinations.goToErrorDialog(
            this@AppointmentListFragment,
            title = R.string.failed_to_sync,
            body = R.string.appointment_update_failed,
            primaryBtn = R.string.retry,
            secondaryBtn = R.string.acknowledge
        )
    }

    private fun scroll() {
        binding?.apply {
            if (appointmentAdapter.itemCount != 0) {
                recyclerView.apply {
                    clearOnScrollListeners()
                    postDelayed({
                        smoothToPosition(lastPosition)
                        this.addOnScrollListener(onScrollListener)
                    }, DELAY_SHORT)
                }
            }
        }
    }

    private fun RecyclerView.smoothToPosition(lastPosition: Int) {
        var toPosition = lastPosition
        if (lastPosition == -1) {
            toPosition = findAppropriatePosition()
        }

        (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(toPosition, 0)
    }

    private fun findAppropriatePosition(): Int {
        var timeOfSelectedDay = LocalDateTime.now()
        var isCurrentDay: Boolean
        lastSelectedDate.apply {
            isCurrentDay = this.isEqual(timeOfSelectedDay.toLocalDate())
            timeOfSelectedDay = timeOfSelectedDay.withYear(this.year).withDayOfYear(this.dayOfYear)
        }
        // DO NOT need to find theClosestAppointment if it's not current day
        if (isCurrentDay.not()) {
            return 0
        }

        val index = appointmentAdapter.findClosestAppointment(timeOfSelectedDay)
        return index.coerceAtLeast(0)
    }

    private fun setupCalendar() {
        binding?.apply {
            viewModel.getAllDatesThatHaveAppointments().observe(viewLifecycleOwner) {
                calendarPicker.setDatesWithData(it)
            }

            calendarContainer.setOnSingleClickListener { it.hide() }
            calendarCard.setOnSingleClickListener { }
            calendarPicker.setOnDateSelectedListener(this@AppointmentListFragment)
            calendarPicker.setSelectedDateAndUpdateMonth(lastSelectedDate)
        }
    }

    private fun debounceLoadAppointment(appointments: List<Appointment>) {
        appointmentListDecoration.timestamp =
            appointments.map { appointment -> appointment.appointmentTime }

        appointmentAdapter.submitList(appointments)

        hideLoading(appointments.isEmpty())

        if (needScrollToCurrentTimePosition) {
            needScrollToCurrentTimePosition = false
            binding?.recyclerView?.post {
                val position = appointmentAdapter.getCurrentDatePosition()
                if (position > 0) {
                    binding?.recyclerView?.smoothScrollToPosition(position)
                }
            }
        }
    }

    private fun checkDateOfScheduleIfNeedReset() {
        val forceRefresh = popResultValue<LocalDate>(APPOINTMENT_DATE)?.let { appointmentDate ->
            Timber.i("Resetting date $appointmentDate")
            lastSelectedDate = appointmentDate
            true
        } ?: false

        updateAppointmentDate(lastSelectedDate, forceRefresh)
    }

    private fun updateAppointmentDate(appointmentDate: LocalDate, forceRefresh: Boolean) {
        binding?.calendarValue?.text = appointmentDate.dayOfMonth.toString()
        binding?.dateTime?.text = appointmentDate.atStartOfDay().toOrdinalDate(" ")
        binding?.calendarPicker?.setSelectedDateAndUpdateMonth(lastSelectedDate)

        if (forceRefresh) {
            viewModel.syncClinicAppointmentsByDate(appointmentDate)
        } else {
            viewModel.updatePatientSchedule(patientSchedule, appointmentDate)
        }
    }

    private fun addProcessingAppointmentCallback() {
        getResultLiveData<Boolean>(ProcessingAppointmentDialog.REFRESH)?.observe(viewLifecycleOwner) {
            if (it) {
                removeResult<Boolean>(ProcessingAppointmentDialog.REFRESH)
                updateAppointmentDate(lastSelectedDate, true)
            }
        }
    }

    private fun showLoading() {
        binding?.apply {
            fabAdd.hide()
            fabLookup.hide()
            emptyMessage.hide()
            recyclerView.hide()
            (preloadView.loading.drawable as? AnimatedImageDrawable)?.start()
            preloadView.textView.isVisible = lastSelectedDate.isToday()
            preloadContainer.isVisible = true
        }
    }

    private fun hideLoading(empty: Boolean) {
        binding?.apply {
            preloadContainer.isGone = true
            fabAdd.show()
            fabLookup.show()
            endLoading()
            if (empty) {
                emptyMessage.text = if (lastSelectedDate.isToday()) {
                    getString(R.string.patient_select_no_schedules)
                } else {
                    getString(R.string.patient_select_no_schedules_other_day)
                }
                emptyMessage.show()
            } else {
                emptyMessage.hide()
                recyclerView.show()
            }
        }
    }
}
