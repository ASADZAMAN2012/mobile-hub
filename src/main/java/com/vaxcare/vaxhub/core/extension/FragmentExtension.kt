/**************************************************************************************************
 * Copyright VaxCare (c) 2019.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController

/**
 * Get result from the back stack as livedata
 *
 * @param T the result type
 * @param id the id of the data element
 * @return the livedata or null
 */
fun <T> Fragment.getResultLiveData(id: String): MutableLiveData<T>? {
    return findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData(id)
}

/**
 * Get result from the back stack
 *
 * @param T the result type
 * @param id the id of the data element
 * @return the data or null
 */
fun <T> Fragment.getResultValue(id: String): T? {
    return findNavController().currentBackStackEntry?.savedStateHandle?.get(id)
}

/**
 * Get result from the back stack and then immediately clears it
 *
 * @param T the result type
 * @param id the id of the data element
 * @return the data or null
 */
fun <T> Fragment.popResultValue(id: String): T? {
    val value: T? = findNavController().currentBackStackEntry?.savedStateHandle?.get(id)
    removeResult<T>(id)
    return value
}

/**
 * Get result from the back stack
 *
 * @param T the result type
 * @param id the id of the data element
 * @return the data or null
 */
fun <T> Fragment.removeResult(id: String): T? {
    return findNavController().currentBackStackEntry?.savedStateHandle?.remove(id)
}

fun Fragment.previousDestinationId(): Int? = findNavController().previousBackStackEntry?.destination?.id

fun Fragment.checkPreviousDestination(destinationId: Int): Boolean =
    findNavController().previousBackStackEntry?.destination?.id == destinationId
