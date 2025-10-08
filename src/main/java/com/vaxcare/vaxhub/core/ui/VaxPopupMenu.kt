/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.vaxcare.vaxhub.databinding.PopupMenuItemBinding
import com.vaxcare.vaxhub.databinding.PopupMenuLayoutBinding

/**
 * Custom popup menu created to fix the Android's nav bar to be shown in Android 10 when using
 * Android's PopupMenu API. Once Blue G50 device is not used, this class is no longer needed,
 * and PopupMenu API can be used.
 *
 * @property context Activity context
 */
class VaxPopupMenu(private val context: Context) :
    PopupWindow(context) {
    private val binding: PopupMenuLayoutBinding =
        PopupMenuLayoutBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT

        // Important! Prevents focus stealing, what causes android nav bar to be shown in Android 10
        isFocusable = false
        // Makes sure the popup is still interactive
        isTouchable = true
        isOutsideTouchable = true

        // Necessary to remove undesired and unexpected black box containing the popup menu
        setBackgroundDrawable(ColorDrawable(Color.WHITE))
        elevation = 6f
    }

    fun addMenuItems(items: List<MenuItem>) {
        items.forEach { item ->
            val itemView = PopupMenuItemBinding.inflate(
                LayoutInflater.from(context),
                binding.menuItemsContainer,
                false
            )
            itemView.textviewTitle.text = item.title
            itemView.root.setOnClickListener {
                item.onClickListener.onClick(it)
                dismiss()
            }
            binding.menuItemsContainer.addView(itemView.root)
        }
    }

    fun clearMenuItems() {
        binding.menuItemsContainer.removeAllViews()
    }

    fun show(anchorView: View) {
        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)

        val popupWidth = 200.dpToPx(context)
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val endMargin = 20.dpToPx(context)
        val topMargin = 4.dpToPx(context)

        val xOffset = when {
            // Adjust x if the popup goes off-screen with the margin
            // This ensures the popup's right edge is at screenWidth - endMargin
            location[0] + popupWidth > screenWidth - endMargin -> {
                screenWidth - endMargin - popupWidth
            }

            else -> {
                // Otherwise, use default x (aligned to anchor's left)
                location[0]
            }
        }

        // Calculate yOffset, ALWAYS below the anchor with margin
        val yOffset = location[1] + anchorView.height + topMargin

        showAtLocation(anchorView, Gravity.NO_GRAVITY, xOffset, yOffset)
    }

    data class MenuItem(val title: String, val onClickListener: View.OnClickListener)
}

private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
