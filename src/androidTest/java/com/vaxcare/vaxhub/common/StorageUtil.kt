/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.EntryPointHelper.getActivity
import com.vaxcare.vaxhub.data.AppDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import javax.inject.Inject

class StorageUtil @Inject constructor() : TestUtilBase() {
    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface StorageUtilEntryPoint {
        fun localStorage(): LocalStorage

        fun database(): AppDatabase
    }

    /**
     *  Clear all the data stored locally
     */
    fun clearLocalStorageAndDatabase() {
        getActivity()?.let {
            val entryPoint = EntryPointAccessors.fromActivity(
                it,
                StorageUtilEntryPoint::class.java
            )

            entryPoint.localStorage().clearData()
            entryPoint.database().clearAllTables()
        }
    }

    /**
     * Clear last user sync date/time
     */
    fun clearUserSync() {
        getActivity()?.let {
            val entryPoint = EntryPointAccessors.fromActivity(
                it,
                StorageUtilEntryPoint::class.java
            )

            entryPoint.localStorage().lastUsersSyncDate = null
        }
    }
}
