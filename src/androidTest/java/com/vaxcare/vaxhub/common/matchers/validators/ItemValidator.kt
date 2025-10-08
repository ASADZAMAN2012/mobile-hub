package com.vaxcare.vaxhub.common.matchers.validators

interface ItemValidator<T> {
    fun validateAtIndex(index: Int, arg: T): Boolean
}
