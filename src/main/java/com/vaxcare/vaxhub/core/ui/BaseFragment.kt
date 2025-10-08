/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.app.Activity
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.datadog.android.rum.GlobalRumMonitor
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenMetric
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.core.ui.extension.invisible
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentBaseBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.EligibilityUiOptions
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.ui.Main
import com.vaxcare.vaxhub.ui.checkout.extensions.setEligibilityContent
import com.vaxcare.vaxhub.ui.checkout.extensions.setEligibilityIcon
import com.vaxcare.vaxhub.ui.navigation.BackNavigationHandler
import com.vaxcare.vaxhub.viewmodel.LocationDataViewModel
import timber.log.Timber

/**
 * This is the base fragment responsible for common rules for the UI.
 *
 * @author Anthony Todd
 * @since 1.0.0
 * @property resource The layout to be inflated by the system
 * @property hasMenu Whether the fragment will show a menu or not
 * @property hasToolbar Whether the system should hide/show the activity's toolbar
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment(), BackNavigationHandler {
    abstract val fragmentProperties: FragmentProperties
    private val screenName: String = this::class.java.name
    protected val fragmentTag = this::class.simpleName ?: "BaseFragment"

    // base binding using in base class
    private var _baseBinding: FragmentBaseBinding? = null
    private val baseBinding
        get() = _baseBinding

    // binding using in the sub class
    private var _binding: VB? = null
    protected val binding: VB?
        get() = _binding

    private val destinationChangeListener =
        NavController.OnDestinationChangedListener { controller, destination, bundle ->
            onDestinationChanged(controller, destination, bundle)
        }

    protected abstract val analytics: AnalyticReport
    protected val locationViewModel: LocationDataViewModel by activityViewModels()

    open val displayLoadingByDefault: Boolean = false

    open fun onLoadingStart() = Unit

    open fun onLoadingStop() = Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView %s", screenName)

        showSystemSettings(
            fragmentProperties.showControlPanel,
            fragmentProperties.showStatusBarIcons
        )

        _baseBinding = FragmentBaseBinding.inflate(inflater, container, false)
        baseBinding?.viewStub?.layoutResource = fragmentProperties.resource
        baseBinding?.let { bb ->
            _binding = bindFragment(bb.viewStub.inflate())
        }

        return baseBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated %s", screenName)

        analytics.saveMetric(ScreenMetric(screenName))

        if (displayLoadingByDefault) {
            startLoading()
        }

        init(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        Timber.d("onStart %s", screenName)

        findNavController().addOnDestinationChangedListener(destinationChangeListener)
    }

    override fun onResume() {
        super.onResume()
        GlobalRumMonitor.get().startView(this, screenName)
        Timber.d("onResume %s", screenName)
    }

    override fun onPause() {
        super.onPause()
        GlobalRumMonitor.get().stopView(this)
        Timber.d("onPause %s", screenName)
    }

    override fun onStop() {
        findNavController().removeOnDestinationChangedListener(destinationChangeListener)

        Timber.d("onStop %s", screenName)

        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("OnDestroyView")
        _binding = null
        _baseBinding = null
    }

    fun startLoading() {
        onLoadingStart()
        baseBinding?.apply {
            loading.show()
            (loading.drawable as? AnimatedImageDrawable)?.start()
        }
    }

    fun showLoadingText(textResource: Int) {
        baseBinding?.apply {
            if (loading.isVisible) {
                loadingText.apply {
                    text = resources.getString(textResource)
                    show()
                }
            }
        }
    }

    fun endLoading() {
        onLoadingStop()
        baseBinding?.apply {
            loading.hide()
            loadingText.invisible()
            (loading.drawable as? AnimatedImageDrawable)?.stop()
        }
    }

    /**
     * function to log screen navigation metric to mixpanel & AI.
     * Ideally this would be enforced in fragment life cycle, but some fragments are used for
     * several screens and we need more context before making this call
     *
     * @param screenTitle
     */
    protected fun logScreenNavigation(screenTitle: String) {
        analytics.saveMetric(ScreenNavigationMetric(screenTitle))
    }

    /**
     * Abstract function to be overridden by the inheritor. Used for running routines during the
     * [onViewCreated] method. This happens after the view has been initialized.
     */
    abstract fun init(view: View, savedInstanceState: Bundle?)

    open fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // At this point we may add authentication here - if the session is not authenticated,
        // redirect to login / pin in screen
    }

    /**
     * get concrete view binding from Subclass
     * @param container the container view
     * @return the view binding
     */
    abstract fun bindFragment(container: View): VB

    open fun canShowConnection(): Boolean = true

    override fun handleBack(): Boolean = false

    protected fun showKeyboard() {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focus = activity?.currentFocus
        focus?.let {
            imm.showSoftInput(it, 0)
        }
    }

    /**
     * A method to hide the soft keyboard when on the screen
     */
    protected fun hideKeyboard() {
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focus = activity?.currentFocus
        focus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    private fun showSystemSettings(showControlPanel: Boolean, showStatusBarIcons: Boolean) {
        val trayLayout = (activity as Main).trayLayout
        trayLayout.isTrayEnabled = showControlPanel
        val controlPanel = (activity as Main).controlPanel
        controlPanel.visibility = if (showControlPanel) View.VISIBLE else View.GONE
        val statusBarIconsLayout = (activity as Main).statusBarIconsLayout
        statusBarIconsLayout.visibility = if (showStatusBarIcons) View.VISIBLE else View.GONE
    }

    fun setEligibilityIcon(
        eligibilityIcon: ImageView,
        appointment: Appointment,
        overridePaymentMode: PaymentMode? = null
    ) {
        eligibilityIcon.setEligibilityIcon(
            options = EligibilityUiOptions(
                appointment = appointment,
                overridePaymentMode = overridePaymentMode,
                inReviewIconOverride = appointment.checkedOut &&
                    appointment.hasShotStatus(ShotStatus.PreShot)
            )
        )
    }

    fun setEligibilityContent(eligibilityMessage: TextView, appointment: Appointment) {
        eligibilityMessage.setEligibilityContent(
            requireContext(),
            appointment
        )
    }
}
