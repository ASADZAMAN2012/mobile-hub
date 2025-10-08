/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getActivity
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.ViewToolbarBinding

class VaxToolbar(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    var onCloseAction: (() -> Unit)? = null
    var onRightIcon1Click: (() -> Unit)? = null
    var onRightIcon2Click: (() -> Unit)? = null

    private val binding: ViewToolbarBinding =
        ViewToolbarBinding.inflate(context.getLayoutInflater(), this, true)

    val toolbarSubInfo by lazy { binding.toolbarSubInfo }
    val toolbarSubIcon by lazy { binding.toolbarSubIcon }
    val rightIcon1 by lazy { binding.rightIcon1 }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.VaxToolbar, 0, 0)
        val title = a.getString(R.styleable.VaxToolbar_title)
        val drawableId = a.getResourceId(R.styleable.VaxToolbar_icon, R.drawable.ic_close)
        val rightDrawable1Id = a.getResourceId(R.styleable.VaxToolbar_right_icon1, -1)
        val rightDrawable2Id = a.getResourceId(R.styleable.VaxToolbar_right_icon2, -1)
        a.recycle()

        binding.toolbarTitle.text = title

        binding.toolbarIcon.apply {
            setImageResource(drawableId)
            setOnSingleClickListener {
                onCloseAction?.let {
                    it()
                } ?: run {
                    getActivity()?.onBackPressed()
                }
            }
        }

        if (rightDrawable1Id != -1) {
            setupRightIcon1(rightDrawable1Id, onRightIcon1Click)
        }
        if (rightDrawable2Id != -1) {
            setupRightIcon2(rightDrawable2Id, onRightIcon2Click)
        }
    }

    fun toolbarIconActive(active: Boolean = true) {
        binding.toolbarIcon.isClickable = active
        if (active) {
            binding.toolbarIcon.show()
        } else {
            binding.toolbarIcon.invisible()
        }
    }

    fun setupRightIcon1(drawableId: Int, onClick: (() -> Unit)?) {
        onRightIcon1Click = onClick
        binding.rightIcon1.setOnSingleClickListener { onRightIcon1Click?.invoke() }
        displayRightIcon(binding.rightIcon1, drawableId)
    }

    fun setupRightIcon2(drawableId: Int, onClick: (() -> Unit)?) {
        onRightIcon2Click = onClick
        binding.rightIcon2.setOnSingleClickListener { onRightIcon2Click?.invoke() }
        displayRightIcon(binding.rightIcon2, drawableId)
    }

    private fun displayRightIcon(rightIcon: ImageView, rightDrawableId: Int) {
        rightIcon.apply {
            if (rightDrawableId > 0) {
                show()
                setImageResource(rightDrawableId)
            } else {
                hide()
            }
        }
    }

    fun highlightRightIcon1(color: Int) {
        val tintColor = ContextCompat.getColor(context, color)
        binding.rightIcon1.imageTintList = ColorStateList.valueOf(tintColor)
    }

    fun setTitle(string: String) {
        binding.toolbarTitle.text = string
    }

    fun setSubTitle(string: String) {
        binding.subHeader.visibility = View.VISIBLE
        binding.toolbarTitle.typeface = ResourcesCompat.getFont(context, R.font.graphik_regular)
        binding.toolbarSubTitle.text = string
    }

    fun onSubTitleClick(onClick: () -> Unit) {
        binding.subHeader.setOnSingleClickListener { onClick() }
    }
}
