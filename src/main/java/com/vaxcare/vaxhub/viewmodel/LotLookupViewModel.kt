/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.BaseMetric
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.inventory.LotNumberWithProduct
import com.vaxcare.vaxhub.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LotLookupViewModel @Inject constructor(
    val productRepository: ProductRepository,
    @MHAnalyticReport val analyticReport: AnalyticReport
) : ViewModel() {
    private val _lotNumberWithProduct = MutableLiveData<LotNumberWithProduct?>(null)
    val lotNumberWithProduct: LiveData<LotNumberWithProduct?> = _lotNumberWithProduct

    fun findLotNumberByName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _lotNumberWithProduct.postValue(
                productRepository.findLotNumberByNameAsync(name)
            )
        }
    }

    fun saveMetric(metric: BaseMetric) {
        analyticReport.saveMetric(metric)
    }
}
