/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T, K, R> LiveData<T>.combineWith(liveData: LiveData<K>, block: (T?, K?) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block.invoke(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block.invoke(this.value, liveData.value)
    }
    return result
}

fun <T, K, R> LiveData<T>.combineWithBoth(liveData: LiveData<K>, block: (T?, K?) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.removeSource(this)
    result.addSource(this) {
        result.removeSource(liveData)
        result.addSource(liveData) {
            result.value = block.invoke(this.value, liveData.value)
        }
    }
    return result
}

fun <T, J, K, R> LiveData<T>.combinePair(
    firstData: LiveData<J>,
    secondData: LiveData<K>,
    block: (T?, J?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block.invoke(this.value, firstData.value, secondData.value)
    }
    result.addSource(firstData) {
        result.value = block.invoke(this.value, firstData.value, secondData.value)
    }
    result.addSource(secondData) {
        result.value = block.invoke(this.value, firstData.value, secondData.value)
    }
    return result
}
