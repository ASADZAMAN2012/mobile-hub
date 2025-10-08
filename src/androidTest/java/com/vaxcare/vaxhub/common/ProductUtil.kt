/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import com.vaxcare.vaxhub.EntryPointHelper.lazyEntryPoint
import com.vaxcare.vaxhub.HiltEntryPointInterface
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.dao.ProductDao
import com.vaxcare.vaxhub.repository.AppointmentRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ProductUtilEntryPoint : HiltEntryPointInterface {
    fun productDao(): ProductDao

    fun appointmentRepository(): AppointmentRepository
}

class ProductUtil() : TestUtilBase() {
    fun findPricePerDoseByProductId(testProduct: TestProducts) =
        runBlocking {
            val entryPoint: ProductUtilEntryPoint by lazyEntryPoint()
            entryPoint.productDao().getOneTouch(testProduct.id)
        }?.selfPayRate?.setScale(2, BigDecimal.ROUND_HALF_UP)?.toDouble() ?: 0.0

//    fun retrieveMedDCopaysByAppointmentId(appointmentId: String) =
//        runBlocking {
//            val entryPoint: ProductUtilEntryPoint by lazyEntryPoint()
//            entryPoint.appointmentRepository().getMedDCopays(appointmentId.toInt())
//        }.copays
}
