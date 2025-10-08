/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.legacy

import android.view.View

class ToastProperties(
    var header: String = "",
    var message: String = "",
    val onClick: ((v: View) -> Unit)? = null,
    val closeAfterMilliseconds: Long? = null
)
