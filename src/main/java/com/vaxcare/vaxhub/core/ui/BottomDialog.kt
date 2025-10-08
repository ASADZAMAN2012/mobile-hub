/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.databinding.DialogBottomsheetBinding
import com.vaxcare.vaxhub.databinding.DialogBottomsheetItemBinding

class BottomDialog : BottomSheetDialogFragment() {
    companion object {
        private const val TITLE = "title"
        private const val DATA = "data"
        private const val SELECTED_INDEX = "selectedIndex"
        private const val ICONS = "icons"

        fun newInstance(
            title: String,
            values: List<String>,
            selectedIndex: Int,
            icons: List<Int>? = null,
            options: BottomDialogOptions? = null
        ): BottomDialog {
            val args = Bundle()
            args.putString(TITLE, title)
            args.putStringArrayList(DATA, ArrayList(values))
            args.putInt(SELECTED_INDEX, selectedIndex)
            args.putIntegerArrayList(ICONS, if (icons == null) ArrayList() else ArrayList(icons))
            val fragment = BottomDialog().apply {
                arguments = args
                dlgOptions = options
            }
            return fragment
        }
    }

    var onSelected: ((index: Int) -> Unit)? = null
    var onDismissed: (() -> Unit)? = null
    var onControlIconSelected: ((Int) -> Unit)? = null
    var dlgOptions: BottomDialogOptions? = null
    var isSelectedItemClickEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransBottomSheetDialogStyle)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (view?.parent as? View)?.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.transparent
            )
        )
        (view?.parent?.parent?.parent as? View)?.fitsSystemWindows = false
    }

    // base binding using in base class
    private var _binding: DialogBottomsheetBinding? = null
    private val binding
        get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogBottomsheetBinding.inflate(inflater, container, false)
        return binding?.root
    }

    /**
     * Delegate to set new values to the adapter list
     */
    fun setItems(newItems: List<String>, newIndex: Int) {
        (binding?.rvBottom?.adapter as? BottomDialogAdapter)?.setValues(newItems, newIndex)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (dlgOptions?.isCancelable == false) {
            binding?.close?.hide()
            isCancelable = false
        } else {
            binding?.close?.setOnClickListener {
                dismiss()
            }
        }

        binding?.title?.text = arguments?.getString(TITLE)
        val selectedItemIndex = arguments?.getInt(SELECTED_INDEX) ?: -1

        binding?.iconContainer?.apply {
            dlgOptions?.controlIconsRes?.let { icons ->
                isVisible = icons.isNotEmpty()

                binding?.iconA?.apply {
                    setImageResource(icons.getOrDefault(0, 0))
                    setOnSingleClickListener {
                        alpha = 1.0f
                        binding?.iconB?.alpha = 0.5f
                        onControlIconSelected?.invoke(0)
                    }
                }

                binding?.iconB?.apply {
                    setImageResource(icons.getOrDefault(1, 0))
                    setOnSingleClickListener {
                        alpha = 1.0f
                        binding?.iconA?.alpha = 0.5f
                        onControlIconSelected?.invoke(1)
                    }
                }
            }
        }

        binding?.subTitle?.apply {
            isVisible = dlgOptions?.stickyItemOpts != null
            dlgOptions?.let { opts ->
                text = opts.stickyItemOpts?.itemValue
                if (opts.stickyItemOpts?.bolded == false || selectedItemIndex != -1) {
                    typeface =
                        ResourcesCompat.getFont(context, R.font.graphik_regular)
                }

                setOnSingleClickListener {
                    onSelected?.invoke(-1)
                    dismiss()
                }
            }
        }

        dlgOptions?.listHeader?.let {
            if (it.isNotBlank()) {
                binding?.listHeader?.text = it
            }
        } ?: binding?.listHeader?.hide()

        if (dlgOptions?.isCondensedHeight == true) {
            binding?.rvBottom?.updatePadding(bottom = 50)
        }

        if (dlgOptions?.isLargerTitle == true) {
            binding?.title?.textSize = 12F
        }

        binding?.rvBottom?.apply {
            dlgOptions?.extraBottomPadding?.let {
                layoutParams = layoutParams.apply {
                    setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + it)
                }
            }
            layoutManager = LinearLayoutManager(context)
            adapter = BottomDialogAdapter(
                values = arguments?.getStringArrayList(DATA) ?: mutableListOf(),
                selectedIndex = selectedItemIndex,
                icons = arguments?.getIntegerArrayList(ICONS) ?: mutableListOf()
            )
            addItemDecoration(BottomDialogDecorator(context))
            dlgOptions?.decoration?.let { addItemDecoration(it) }
        }
    }

    /**
     * Get a value or default instead of throwing out of bounds exception
     */
    private fun <T> List<T>.getOrDefault(index: Int, default: T): T {
        return if (index >= size) default else this[index]
    }

    inner class BottomDialogAdapter(
        val values: MutableList<String>,
        private var selectedIndex: Int,
        private val icons: MutableList<Int>
    ) : RecyclerView.Adapter<BottomDialogHolder>() {
        override fun getItemCount() = values.size

        /**
         * Re-set the values from outside source
         */
        fun setValues(newValues: List<String>, newIndex: Int) {
            values.clear()
            values.addAll(newValues)
            selectedIndex = newIndex
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BottomDialogHolder {
            return BottomDialogHolder(
                DialogBottomsheetItemBinding.inflate(parent.getInflater(), parent, false)
            )
        }

        override fun onBindViewHolder(holder: BottomDialogHolder, position: Int) {
            holder.bind(
                values[position],
                selectedIndex,
                position,
                if (position < icons.size) icons[position] else null
            )
        }
    }

    inner class BottomDialogHolder(val binding: DialogBottomsheetItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            value: String,
            selectedIndex: Int,
            position: Int,
            imageRes: Int?
        ) {
            with(itemView) {
                val (labelText, subLabelText) = if (value.contains("\n")) {
                    value.split("\n")[0] to value.split("\n")[1]
                } else {
                    value to null
                }
                binding.label.text = labelText
                subLabelText?.let {
                    binding.subLabel.isVisible = true
                    binding.subLabel.text = it
                }
                if (selectedIndex == position) {
                    binding.label.typeface =
                        ResourcesCompat.getFont(context, R.font.graphik_semi_bold)
                } else {
                    binding.label.typeface =
                        ResourcesCompat.getFont(context, R.font.graphik_regular)
                }

                if (isSelectedItemClickEnabled || selectedIndex != position) {
                    binding.root.setOnClickListener {
                        onSelected?.invoke(position)
                        dismiss()
                    }
                } else {
                    binding.root.isClickable = false
                }

                imageRes?.let {
                    binding.icon.setImageResource(it)
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // required on fragment
        _binding = null
    }
}

/**
 * Options for a more versatile experience
 *
 * @property stickyItemOpts Options for sticky item
 * @property listHeader Header option for a sort of "sub-subtitle" when necessary
 * @property decoration Optional ItemDecoration for items
 * @property controlIconsRes Optional list (up to 2) for icon resources when necessary
 * @property extraBottomPadding Optional padding to apply to the RecyclerView bottom padding
 */
data class BottomDialogOptions(
    val stickyItemOpts: StickyItemOptions? = null,
    val listHeader: String? = "",
    val decoration: ItemDecoration? = null,
    val controlIconsRes: List<Int> = emptyList(),
    val isCancelable: Boolean = true,
    val isCondensedHeight: Boolean = false,
    val isLargerTitle: Boolean = false,
    val extraBottomPadding: Int? = null
)

/**
 * Options for Sticky Item
 *
 * @property itemValue Display text
 * @property bolded Text should use the graphik_semi-bold font
 */
data class StickyItemOptions(
    val itemValue: String,
    val bolded: Boolean
)
