/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui

import android.animation.LayoutTransition
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.work.WorkManager
import com.microsoft.appcenter.crashes.Crashes
import com.vaxcare.core.report.Reporting
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.AppStartedMetric
import com.vaxcare.core.report.model.NetworkMetric
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.core.ui.extension.hide
import com.vaxcare.core.ui.extension.show
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.constant.Receivers.UPDATE_REQUEST_CODE
import com.vaxcare.vaxhub.core.extension.getAttributeInt
import com.vaxcare.vaxhub.core.extension.hideSystemUi
import com.vaxcare.vaxhub.core.extension.registerBroadcastReceiver
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BaseOverlay
import com.vaxcare.vaxhub.core.view.TrayListener
import com.vaxcare.vaxhub.databinding.ActivityMainBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.SignalStrengthLevel
import com.vaxcare.vaxhub.model.legacy.ToastProperties
import com.vaxcare.vaxhub.model.metric.BatteryMetric
import com.vaxcare.vaxhub.service.BatteryMonitor
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.admin.ControlPanelView
import com.vaxcare.vaxhub.ui.navigation.BackNavigationHelper
import com.vaxcare.vaxhub.ui.navigation.MainDestination
import com.vaxcare.vaxhub.update.InAppUpdates
import com.vaxcare.vaxhub.viewmodel.AdminSetupState
import com.vaxcare.vaxhub.viewmodel.AdminViewModel
import com.vaxcare.vaxhub.viewmodel.MainViewModel
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import com.vaxcare.vaxhub.worker.WorkerBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * This is the main Activity that will drive all UI decisions. This class will be responsible for
 * the navigation drawer, the toolbar title (but not the menu), as well as the content that is
 * being drawn on the screen. The [mainViewModel] will use the Activity's lifecycle. There should be
 * no other Activities for this application.
 *
 * @author Anthony Todd
 * @since 1.0.0
 */
@AndroidEntryPoint
class Main : AppCompatActivity() {
    /**
     *  The last network status, so that we can determine what message to show for reconnection
     */
    var lastNetworkStatus =
        NetworkMonitor.NetworkStatusShownUpdate(
            ssid = null,
            networkStatus = NetworkStatus.DISCONNECTED,
            signalStrengthLevel = SignalStrengthLevel.NO_INTERNET
        )

    /**
     * The main view model that will drive communication between all of the children. This should
     * handle the current state of the application. This will use the Activity's lifecycle and will
     * be destroyed when the activity is destroyed.
     */
    private val mainViewModel: MainViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()

    @Inject
    lateinit var destination: MainDestination

    /**
     * Reporting
     */
    @Inject
    lateinit var reporting: Reporting

    @Inject
    @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    @Inject
    lateinit var localStorage: LocalStorage

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var batteryMonitor: BatteryMonitor

    @Inject
    lateinit var inAppUpdates: InAppUpdates

    private val flags = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LOW_PROFILE
    )

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val navHost by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host)
    }

    val trayLayout by lazy { binding.trayLayout }
    val controlPanel by lazy { binding.controlPanel }
    val statusBarIconsLayout by lazy { binding.statusBarIcons.statusBarIconsLayout }
    private lateinit var mDetector: GestureDetectorCompat
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            inAppUpdates.startFlow(this@Main)
        }
    }

    /**
     * This is the main navigation controller to draw the main content
     */
    private val navCtrl by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
    }

    private val backNavigator: BackNavigationHelper by lazy {
        BackNavigationHelper(navCtrl)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            Timber.i("onActivityResult update: resultCode: $resultCode")
            if (resultCode != RESULT_OK) {
                inAppUpdates.startFlow(this@Main)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reporting.start()

        reporting.configure(mainViewModel.deviceSerialNumber)
        analytics.configure(mainViewModel.deviceSerialNumber)

        analytics.saveMetric(AppStartedMetric())

        if (localStorage.getDarkModeEnabled()) {
            setTheme(R.style.VaxCareThemeDark)
        }
        setContentView(binding.root)
        configureToolbar()
        checkIsSetup()
        setupNetworkListeners()

        setupControlPanelListeners()

        if (BuildConfig.DEBUG || BuildConfig.BUILD_TYPE == "qa") {
            binding.buildVersion.text =
                String.format(
                    Locale.getDefault(),
                    getString(R.string.debug_version),
                    BuildConfig.BUILD_TYPE,
                    BuildConfig.VERSION_CODE,
                    BuildConfig.VERSION_NAME
                )

            mDetector = GestureDetectorCompat(this, GestureListener(analytics))
            binding.buildVersion.setOnTouchListener { _, event ->
                return@setOnTouchListener mDetector.onTouchEvent(event)
            }
        }

        localStorage.registeredDoorSensor = ""
    }

    override fun onStart() {
        super.onStart()
        registerUpdateReceiver()
    }

    /**
     * We need to hide the UI whenever we navigate back to this Activity. Since the open source
     * license breaks immersive mode.
     */
    override fun onResume() {
        Timber.d("OnResume")
        super.onResume()

        hideSystemUi()
    }

    override fun onStop() {
        if (navCtrl.currentDestination?.id != R.id.splashFragment) {
            destination.lockScreen(this)
        }
        unregisterUpdateReceiver()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        // flush the analytics to the server
        analytics.flushMetrics()
    }

    /**
     * Keep the user from leaving the application when they are on the Login Fragment. Also close
     * the drawer if it is open before processing any back button actions.
     *
     */
    override fun onBackPressed() {
        backNavigator.handleBackPressed(navHost?.childFragmentManager?.primaryNavigationFragment)
    }

    /**
     * When we have focus on the window, we want to disable the system UI. This is used for the soft
     * keyboard, since it disables immersive mode when invoked.
     *
     * TODO This is still not 100% fixed. We need to figure out why the keyboard won't lose focus
     *
     * @param hasFocus
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
            Timber.d("Hide the system Ui now that we have focus")
        } else {
            try {
                val wmgClass = Class.forName("android.view.WindowManagerGlobal")
                val wmgInstance = wmgClass.getMethod("getInstance").invoke(null)
                val viewsField = wmgClass.getDeclaredField("mViews")
                viewsField.isAccessible = true

                val views = viewsField.get(wmgInstance) as ArrayList<View>
                // When the popup appears, its decorView is the peek of the stack aka last item
                views.last().apply {
                    systemUiVisibility = flags
                    setOnSystemUiVisibilityChangeListener {
                        systemUiVisibility = flags
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Configure the support action bar / toolbar to handle the UI as needed. We also need to hide
     * the toolbar when we load since it's not present for the Main login screen and setup the
     * animations that will be used for showing/hiding the toolbar.
     *
     * @see [AppCompatActivity.getSupportActionBar
     */
    private fun configureToolbar() {
        binding.fragment.layoutTransition.apply {
            enableTransitionType(LayoutTransition.CHANGING)
            setDuration(getAttributeInt(R.attr.animationShort).toLong())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * We want to check that our user is actually setup up. If they are, proceed with the login
     * screen. Otherwise, put them on the setting screen and kill the backstack.
     *
     * If we are setup, check that we have a door sensor and load the foreground service.
     * Load the network monitor class to keep tabs on our network.
     */
    private fun checkIsSetup() {
        adminViewModel.setupState.observe(this) { state ->
            when (state) {
                AdminSetupState.NOT_SETUP -> WorkerBuilder.destroy(this@Main)
                AdminSetupState.SETUP, AdminSetupState.CONFIGURED -> reInitWorkers()
                else -> Unit
            }
        }
    }

    /**
     * Re-initialize WorkerBuilder - This should only be done when the hub is set up / configured
     */
    private fun reInitWorkers() {
        WorkerBuilder.destroy(this@Main)
        WorkerBuilder.initialize(this@Main)
    }

    @Inject
    lateinit var listener: HiltWorkManagerListener

    private fun setupNetworkListeners() {
        lifecycleScope.launch {
            networkMonitor.networkStatusForUI
                .zip(batteryMonitor.asFlow()) { (networkStatus, netUpdate), battery ->
                    Triple(
                        networkStatus,
                        netUpdate,
                        battery
                    )
                }
                .collectLatest { (status, netUpdate, battery) ->
                    status?.let {
                        Timber.d("Network Status: $status")
                        if (status == NetworkStatus.CONNECTED) {
                            OneTimeWorker.buildOneTimeUniqueWorker(
                                wm = WorkManager.getInstance(this@Main),
                                parameters = OneTimeParams.OfflineRequest,
                                listener = listener
                            )
                        }

                        analytics.saveMetric(
                            NetworkMetric(
                                currentStatus = status.name,
                                security = networkMonitor.networkProperties.value?.security,
                                frequency = networkMonitor.networkProperties.value?.frequency,
                                powerSavingMode = battery?.isOnPowerSavingMode
                            )
                        )
                    }

                    netUpdate?.let {
                        binding.controlPanel.updateNetworkStatus(netUpdate)
                        binding.statusBarIcons.statusBarWireless.setImageResource(netUpdate.signalStrengthLevel.icon)
                        lastNetworkStatus = netUpdate
                    }

                    battery?.let {
                        analytics.saveMetric(
                            BatteryMetric(
                                percent = it.percent,
                                isConnected = it.Connected,
                                powerSavingMode = it.isOnPowerSavingMode
                            )
                        )
                    }
                }
        }
    }

    private fun setupControlPanelListeners() {
        binding.trayLayout.shouldInterceptTouch = { event ->
            binding.controlPanel.shouldInterceptTouchEvent(event)
        }

        networkMonitor.cellularLevel.observe(this) { level ->
            binding.statusBarIcons.apply {
                if (level == -1) {
                    statusBarCellular.hide()
                    statusBarCellular.setImageResource(R.drawable.ic_cellular_disconnected)
                    return@apply
                }

                statusBarCellular.show()
                when (level) {
                    0, 1 -> statusBarCellular.setImageResource(R.drawable.ic_cellular_level_weak)
                    2 -> statusBarCellular.setImageResource(R.drawable.ic_cellular_level_medium)
                    else -> statusBarCellular.setImageResource(R.drawable.ic_cellular_level_strong)
                }
            }
        }

        binding.trayLayout.addTrayListener(object : TrayListener {
            override fun onTrayOpened() {
                // We start check the network status when the control panel opened.
                networkMonitor.startCheckConnectionTask()
                binding.controlPanel.forceUpdateConnectivityIconsTint()
            }

            override fun onTrayClosed() {
                binding.controlPanel.forceUpdateConnectivityIconsTint()
                if (navCtrl.currentDestination?.id == R.id.splashFragment) {
                    binding.statusBarIcons.root.visibility = View.VISIBLE
                }
            }

            override fun onTrayStartDrag() {
                binding.statusBarIcons.root.visibility = View.GONE
            }
        })

        val listener = object : ControlPanelView.Listener {
            override fun onStartAdminFlow() {
                binding.trayLayout.closeTray()

                when (navCtrl.currentDestination?.id) {
                    R.id.splashFragment, R.id.pinLockFragment, R.id.appointmentListFragment ->
                        destination.goToAdminLogin(this@Main)
                }
            }

            override fun onCloseTrayLayout() {
                binding.trayLayout.closeTray()
            }

            override fun onUpdateStatusBarBatteryInfo(batteryDrawable: Drawable, percent: String) {
                binding.statusBarIcons.statusBarBatteryIcon.setImageDrawable(batteryDrawable)
                binding.statusBarIcons.statusBarBatteryPercent.text = percent
            }

            override fun onUpdateStatusBarNetworkInfo(connectivityDrawable: Drawable, wirelessDrawable: Drawable) {
                binding.statusBarIcons.statusBarSystemConnectivity.setImageDrawable(
                    connectivityDrawable
                )
            }
        }

        binding.controlPanel.setListeners(
            this,
            networkMonitor,
            batteryMonitor,
            listener
        )
    }

    /**
     * A lot of fragments that we DO NOT want toast notifications to appear on.
     * include:
     * R.id.PinLockFragment, R.id.AdminSetupOverlayDialog,
     * R.id.ScannedDoseIssueDialog, R.id.CheckoutPatientFragment, R.id.CheckoutSummaryFragment,
     * R.id.CheckoutCompleteFragment, R.id.RemoveDoseDialog
     *
     * @return
     */
    private fun canShowConnectionToast(): Boolean {
        return when (val fragment = navHost?.childFragmentManager?.fragments?.lastOrNull()) {
            is BaseFragment<*> -> fragment.canShowConnection()
            is BaseOverlay<*> -> fragment.canShowConnection()
            else -> true
        }
    }

    fun showToastMessage(
        headerResId: Int,
        messageResId: Int?,
        durationInMillis: Long? = null
    ) {
        val header = resources.getString(headerResId)
        val message = messageResId?.let { resources.getString(it) } ?: ""
        val properties = ToastProperties(
            header = header,
            message = message,
            closeAfterMilliseconds = durationInMillis
        )
        binding.toastNotifications.showToast(properties)
    }

    private fun registerUpdateReceiver() {
        registerBroadcastReceiver(
            receiver = updateReceiver,
            intentFilter = IntentFilter(Receivers.IN_APP_UPDATE_ACTION)
        )
    }

    private fun unregisterUpdateReceiver() {
        unregisterReceiver(updateReceiver)
    }

    class GestureListener(
        @MHAnalyticReport val analytics: AnalyticReport
    ) :
        GestureDetector.SimpleOnGestureListener() {
        // must override onDown and return true for other gestures to function properly
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            analytics.flushMetrics()
            Crashes.generateTestCrash()
        }
    }
}
