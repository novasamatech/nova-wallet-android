package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model

import android.os.Parcelable
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.sign.SignInterScreenCommunicator
import io.novafoundation.nova.runtime.extrinsic.ValidityPeriod
import kotlinx.android.parcel.Parcelize

@Parcelize
class ScanSignParitySignerPayload(
    val request: SignInterScreenCommunicator.Request,
    val validityPeriod: ValidityPeriodParcel,
    val variant: PolkadotVaultVariant,
) : Parcelable

@Parcelize
class ValidityPeriodParcel(
    val periodInMillis: Long,
    val calculatedAt: Long
) : Parcelable

fun mapValidityPeriodToParcel(validityPeriod: ValidityPeriod): ValidityPeriodParcel {
    return ValidityPeriodParcel(
        validityPeriod.period.millis,
        validityPeriod.period.millisCalculatedAt
    )
}

fun mapValidityPeriodFromParcel(validityPeriodParcel: ValidityPeriodParcel): ValidityPeriod {
    return ValidityPeriod(
        TimerValue(
            millis = validityPeriodParcel.periodInMillis,
            millisCalculatedAt = validityPeriodParcel.calculatedAt
        )
    )
}
