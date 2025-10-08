/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogDoseSeriesSelectionBinding
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseSeriesAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseSeriesClickListener
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class SelectDoseSeriesDialog :
    BaseDialog<DialogDoseSeriesSelectionBinding>(),
    DoseSeriesClickListener {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    companion object {
        const val SELECTED_VACCINE_ADAPTER_MODEL_ID = "SELECTED_VACCINE_ADAPTER_MODEL_ID"
        const val SELECTED_DOSE_SERIES = "SELECTED_DOSE_SERIES"
        const val DOSE_SERIES_CHANGED = "DOSE_SERIES_CHANGED"
    }

    private val args: SelectDoseSeriesDialogArgs by navArgs()

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogDoseSeriesSelectionBinding =
        DialogDoseSeriesSelectionBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.doseSeriesRecycler?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter =
                DoseSeriesAdapter(args.dosesInSeries, args.doseSeries, this@SelectDoseSeriesDialog)
        }
    }

    override fun onItemClicked(doseValue: Int) {
        globalDestinations.goBack(
            this@SelectDoseSeriesDialog,
            mapOf(
                DOSE_SERIES_CHANGED to
                    JSONObject().apply {
                        put(SELECTED_VACCINE_ADAPTER_MODEL_ID, args.selectedVaccineId)
                        put(SELECTED_DOSE_SERIES, doseValue)
                    }.toString()
            )
        )
    }
}
