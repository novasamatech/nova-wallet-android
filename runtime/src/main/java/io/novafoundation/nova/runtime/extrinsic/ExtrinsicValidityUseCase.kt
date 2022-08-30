package io.novafoundation.nova.runtime.extrinsic

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.runtime.R
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class ValidityPeriod(val period: TimerValue)

fun ValidityPeriod.closeToExpire(): Boolean {
    val currentTimer = System.currentTimeMillis()
    val passedTime = currentTimer - period.millisCalculatedAt
    val remainingTIme = period.millis - passedTime

    return remainingTIme.milliseconds < 1.minutes
}

interface ExtrinsicValidityUseCase {

    suspend fun extrinsicValidityPeriod(payload: SignerPayloadExtrinsic): ValidityPeriod
}

internal class RealExtrinsicValidityUseCase(
    private val mortalityConstructor: MortalityConstructor
) : ExtrinsicValidityUseCase {

    override suspend fun extrinsicValidityPeriod(payload: SignerPayloadExtrinsic): ValidityPeriod {
        val timerValue = TimerValue(
            millis = mortalityConstructor.mortalPeriodMillis(),
            millisCalculatedAt = System.currentTimeMillis()
        )

        return ValidityPeriod(timerValue)
    }
}

fun LifecycleOwner.startExtrinsicValidityTimer(
    validityPeriod: ValidityPeriod,
    @StringRes timerFormat: Int,
    timerView: TextView,
    onTimerFinished: () -> Unit
) {
    timerView.startTimer(
        value = validityPeriod.period,
        customMessageFormat = timerFormat,
        lifecycle = lifecycle,
        onTick = { view, _ ->
            val textColorRes = if (validityPeriod.closeToExpire()) R.color.red else R.color.white_64

            view.setTextColorRes(textColorRes)
        },
        onFinish = { onTimerFinished() }
    )
}
