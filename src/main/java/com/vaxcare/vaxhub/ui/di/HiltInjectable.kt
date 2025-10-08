/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.di

import com.vaxcare.vaxhub.core.model.BaseHiltDataStore

/**
 * Interface for using Datastore for Dependency Injection
 */
interface HiltInjectable {
    /**
     * @return Datastore containing all dependencies
     */
    fun getDataStore(): BaseHiltDataStore
}

inline fun <reified T> HiltInjectable.getDependency(): T {
    return getDataStore()[T::class.java]
}
