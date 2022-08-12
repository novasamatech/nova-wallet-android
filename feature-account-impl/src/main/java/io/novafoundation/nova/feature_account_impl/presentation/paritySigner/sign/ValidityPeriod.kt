package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign

import io.novafoundation.nova.common.utils.formatting.TimerValue
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes

class ValidityPeriod(val period: TimerValue)

@OptIn(ExperimentalTime::class)
fun ValidityPeriod.closeToExpire(): Boolean {
    val currentTimer = System.currentTimeMillis()
    val passedTime = currentTimer - period.millisCalculatedAt
    val remainingTIme = period.millis - passedTime

    return remainingTIme.milliseconds < 1.minutes
}
