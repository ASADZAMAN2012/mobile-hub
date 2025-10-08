/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.OverlayProperties
import com.vaxcare.vaxhub.databinding.OverlayFullBinding

abstract class BaseOverlay<VB : ViewBinding> : DialogFragment() {
    abstract val baseOverlayProperties: OverlayProperties

    private val screenName: String = this::class.java.name

    // base binding using in base class
    private var baseBinding: OverlayFullBinding? = null

    // binding using in the sub class
    protected var binding: VB? = null

    /**
     * Before we call the dialog, we need to set the flags on the dialog to FLAG_NOT_FOCUSABLE so
     * we don't trigger a "user" interaction.
     *
     * @see hideUi
     */
    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        dialog.setCanceledOnTouchOutside(false)
    }

    /**
     * @suppress
     */
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            setBackgroundDrawableResource(R.color.primary_white)
        }
    }

    /**
     * On show, call [hideUi]
     *
     * @see hideUi
     */
    override fun show(manager: FragmentManager, tag: String?) {
        hideUi(manager)
        super.show(manager, tag)
    }

    /**
     * On show, call [hideUi]
     *
     * @see hideUi
     */
    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        val result = super.show(transaction, tag)
        hideUi(parentFragmentManager)
        return result
    }

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
        baseBinding?.apply {
            if (baseOverlayProperties.showHeader) {
                headerTitle.text = baseOverlayProperties.headerTitle
                headerTitle.gravity = baseOverlayProperties.headerGravity
                header.show()
            } else {
                header.invisible()
                buttonBack.invisible()
            }

            if (baseOverlayProperties.showHeader && baseOverlayProperties.showBack) {
                buttonBack.show()

                buttonBack.setOnClickListener {
                    dialog?.dismiss()
                }
            } else {
                buttonBack.invisible()
            }

            if (baseOverlayProperties.showX) {
                buttonCloseDialog.show()
                if (!baseOverlayProperties.showHeader) {
                    buttonCloseDialog.setColorFilter(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.darkBlue
                        ),
                        android.graphics.PorterDuff.Mode.MULTIPLY
                    )
                }
                buttonCloseClickListener(root)
            } else {
                buttonCloseDialog.invisible()
            }
            content.show()
            dialogContent.hide()
        }

        baseBinding?.let { bb -> binding = bindFragment(inflater, bb.content) }
        return baseBinding?.root
    }

    protected open fun buttonCloseClickListener(rootView: ConstraintLayout) {
        baseBinding?.buttonCloseDialog?.setOnClickListener {
            dialog?.dismiss()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }

    /**
     * After view inflation, run any code by the caller in the [init] function.
     *
     * @see init
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // required on fragment
        binding = null
        baseBinding = null
    }

    /**
     * Because dialogs trigger a "user" interaction when being created, the immersive mode will
     * break and both nav bars will appear while the dialog is open. To counteract that, we have to
     * execute all pending transactions on the fragment to get the latest window (this will ensure
     * it's not null) and then copy the flags from the activity's systemUi to the dialog. Once we
     * have copied over the flags, we can clear the focusable flag that was set during the
     * [setupDialog] routine.
     *
     * Add a boolean for resuming to make sure we don't try to execute pending transactions twice.
     *
     * A deeper explanation can be found at [stackoverflow](https://stackoverflow.com/questions/22794049/how-do-i-maintain-the-immersive-mode-in-dialogs/23207365#23207365)
     *
     * @param manager the fragment manager for the current stack
     * @param resuming if we are currently resuming
     * @see setupDialog
     */
    protected fun hideUi(manager: FragmentManager?, resuming: Boolean = false) {
        try {
            if (!resuming) {
                manager?.executePendingTransactions()
            }
            dialog?.window?.decorView?.systemUiVisibility =
                activity?.window?.decorView?.systemUiVisibility ?: 0
            dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        } catch (e: IllegalStateException) {
            // Swallow the exception. Turns out that if you try to hide the ui in the on resume
            // portion of the app, the keyboard stops working...
            e.printStackTrace()
        }
    }

    /**
     * Abstract class to be overridden by the inheritor. Used for running routines during the
     * [onViewCreated] method. This happens after the view has been initialized.
     *
     * @param view the view created
     * @param savedInstanceState optional saved instance state
     */
    abstract fun init(view: View, savedInstanceState: Bundle?)

    /**
     * get concrete view binding from Subclass
     * @param inflater
     * @param container
     * @return
     */
    abstract fun bindFragment(inflater: LayoutInflater, container: ViewGroup): VB

    open fun canShowConnection(): Boolean = true
}
