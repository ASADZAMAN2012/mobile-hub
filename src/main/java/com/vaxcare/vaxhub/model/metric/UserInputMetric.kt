/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.BaseMetric

abstract class UserInputMetric(private val userInputEventType: String) : BaseMetric() {
    override var eventName: String = "UserInput.$userInputEventType"
}
