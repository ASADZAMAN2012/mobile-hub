/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.viewbinding.ViewBinding
import com.datadog.android.rum.GlobalRumMonitor
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.OverlayProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.databinding.OverlayFullBinding
import com.vaxcare.vaxhub.ui.navigation.BackNavigationHandler

/**
 * This is the base dialog class for implementing custom dialogs throughout the application. By
 * default, the [contentResource] is required and the width/height will be match_parent with some
 * padding on each side
 *
 * There is some "Jankiness" happening in this class to make sure we stay in immersive mode when we
 * are navigating to a dialog. See [setupDialog] and both [show] methods.
 *
 * An example of setting up this in a class titled 'TestDialog':
 *
 * ```
 * class TestDialog : BaseDialog(
 *   contentResource = R.layout.dialog_test,
 *   subHeaderResource = R.layout.dialog_sub_header_test
 *   ) {
 *
 *   override fun init(view: View, savedInstanceState: Bundle?) {
 *   setHeaders("AVR Order #12809128912", "Order Details")
 *   }
 * }
```
 *
 * @author Anthony Todd <atodd@vaxcare.com>
 * @since 1.0.0
 * @property contentResource the layout that you want to inflate into the content view of the dialog
 * @property subHeaderResource the sub header layout you want to inflate. Note: if this is null,
 *      sub header will be Visibility.GONE
 * @property actionBarResource the layout for the action bar. will throw if it has not been set.
 * @property width the width of the dialog in pixels
 * @property height the width of the dialog in pixels
 * @see setupDialog
 * @see show
 */
abstract class BaseDialog<VB : ViewBinding> : BaseOverlay<VB>(), ActionBar, BackNavigationHandler {
    abstract val baseDialogProperties: DialogProperties

    override val baseOverlayProperties: OverlayProperties =
        OverlayProperties()

    // base binding using in base class
    private var baseBinding: OverlayFullBinding? = null

    /**
     * @suppress
     */
    private var actionBarVisible: Boolean = false

    /**
     * The action bar view required for referencing views on the layout. Will throw if the view has
     * not been set.
     */
    val actionBar: View?
        get() {
            return baseBinding?.actionBar
        }

    /**
     * @suppress
     */
    private val wm by lazy { context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    override fun handleBack(): Boolean = false

    /**
     * The standard method used by Android to create the view. We inflate 2 source files and attach
     * them to the parent. The [contentResource] is required and always attached, but the
     * [subHeaderResource] is not required and can be ignored if not present. See
     * [Dialog#onCreateView](https://developer.android.com/reference/androidx/fragment/app/Fragment.html#onCreateView(android.view.LayoutInflater,%20android.view.ViewGroup,%20android.os.Bundle))
     * for more information about onCreateView.
     *
     * @see [Dialog#onCreateView](https://developer.android.com/reference/androidx/fragment/app/Fragment.html#onCreateView(android.view.LayoutInflater,%20android.view.ViewGroup,%20android.os.Bundle))
     */
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        baseBinding = OverlayFullBinding.inflate(inflater, container, false)

        val lp = baseBinding?.dialogContent?.layoutParams
        val size = Point()
        wm.defaultDisplay.getSize(size)

        when (baseDialogProperties.dialogSize) {
            DialogSize.LARGE -> {
                lp?.height = size.y - 200 // baseOverlayProperties.height
                lp?.width = size.x - 300 // baseOverlayProperties.width
            }

            DialogSize.MEDIUM -> {
                lp?.width = resources.getDimension(R.dimen.dialog_width_medium).toInt()
                lp?.height = resources.getDimension(R.dimen.dialog_height_medium).toInt()
            }

            DialogSize.SMALL -> {
                lp?.width = resources.getDimension(R.dimen.dialog_width_small).toInt()
                lp?.height = resources.getDimension(R.dimen.dialog_height_small).toInt()
            }

            DialogSize.LEGACY_TRANSACTION -> {
                lp?.width = resources.getDimension(R.dimen.legacy_dialog_width_transaction).toInt()
                lp?.height =
                    resources.getDimension(R.dimen.legacy_dialog_height_transaction).toInt()
            }

            DialogSize.LEGACY_TRANSACTION_LARGER_HEIGHT -> {
                lp?.width = resources.getDimension(R.dimen.legacy_dialog_width_transaction).toInt()
                lp?.height =
                    resources.getDimension(R.dimen.legacy_dialog_height_transaction_larger).toInt()
            }

            DialogSize.LEGACY_TRANSACTION_SMALL -> {
                lp?.width =
                    resources.getDimension(R.dimen.legacy_dialog_width_transaction_small).toInt()
                lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            DialogSize.LEGACY_TRANSACTION_WRAP -> {
                lp?.width = ViewGroup.LayoutParams.WRAP_CONTENT
                lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
                baseBinding?.dialogContent?.background = null
            }

            DialogSize.SMALL_WIDTH -> {
                lp?.width = resources.getDimension(R.dimen.dialog_width_small).toInt()
                lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            DialogSize.LEGACY_WRAP -> {
                lp?.width =
                    resources.displayMetrics.widthPixels - resources.getDimension(R.dimen.dp_22)
                        .toInt()
                lp?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }

            DialogSize.MATCH_PARENT -> {
                lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp?.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        baseBinding?.content?.layoutParams = lp

        if (baseDialogProperties.actionBarResource != null) {
            inflater.inflate(baseDialogProperties.actionBarResource!!, baseBinding?.actionBar, true)
        }

        baseBinding?.dialogContent?.let {
            binding = bindFragment(inflater, it)
        }
        if (baseDialogProperties.adjustKeyboard) {
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        disableNativeBackButton()
        return baseBinding?.root
    }

    /**
     * Block out Back button
     */
    fun disableNativeBackButton() = dialog?.setCancelable(false)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(R.color.dimTransparent)
        }
    }

    /**
     * Hide the UI and if the action bar was visible, show the action bar.
     */
    override fun onResume() {
        super.onResume()
        GlobalRumMonitor.get().startView(this, this::class.java.name)
        hideUi(parentFragmentManager, true)
    }

    override fun onPause() {
        super.onPause()
        GlobalRumMonitor.get().stopView(this)
    }

    /**
     * Show the action bar for the user. Animate by default, but disable if requested.
     */
    override fun showActionBar(animate: Boolean) {
        if (!actionBarVisible) {
            val animationIn = AnimationUtils.loadAnimation(context, R.anim.action_bar_slideup)
            animationIn.duration = 200
            baseBinding?.actionBar?.show()
            baseBinding?.actionBar?.startAnimation(animationIn)
            actionBarVisible = true
        }
    }

    /**
     * Hide the action bar and if we are pausing, note that we are pausing and store that so when we
     * revisit the dialog, we can repopulate the action bar.
     *
     * @param pausing
     */
    override fun hideActionBar(pausing: Boolean) {
        if (actionBarVisible) {
            val animationOut = AnimationUtils.loadAnimation(context, R.anim.action_bar_slidedown)
            animationOut.duration = 200
            baseBinding?.actionBar?.hide()
            baseBinding?.actionBar?.startAnimation(animationOut)
            actionBarVisible = pausing
        }
    }

    companion object {
        const val DIALOG_RESULT = "DIALOG_RESULT"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // required on fragment
        baseBinding = null
    }
}
