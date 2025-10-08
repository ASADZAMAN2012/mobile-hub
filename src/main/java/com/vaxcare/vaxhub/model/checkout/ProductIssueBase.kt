/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.checkout

/**
 * Base ProductIssue class overriding compareTo to use the weight from Weighable
 *
 * @see Weighable
 */
abstract class ProductIssueBase : Weighable, Comparable<Weighable> {
    override fun compareTo(other: Weighable): Int = this.weight.compareTo(other.weight)
}
