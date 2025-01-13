package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.CollatorParcelModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger

@Parcelize
class YieldBoostConfirmPayload(
    val collator: CollatorParcelModel,
    val configurationParcel: YieldBoostConfigurationParcel,
    val fee: FeeParcelModel,
) : Parcelable

sealed class YieldBoostConfigurationParcel(open val collatorIdHex: String) : Parcelable {

    @Parcelize
    class On(
        val threshold: BigInteger,
        val frequencyInDays: Int,
        override val collatorIdHex: String
    ) : YieldBoostConfigurationParcel(collatorIdHex)

    @Parcelize
    class Off(override val collatorIdHex: String) : YieldBoostConfigurationParcel(collatorIdHex)
}

fun YieldBoostConfigurationParcel(configuration: YieldBoostConfiguration) = when (configuration) {
    is YieldBoostConfiguration.On -> YieldBoostConfigurationParcel.On(
        threshold = configuration.threshold,
        frequencyInDays = configuration.frequencyInDays,
        collatorIdHex = configuration.collatorIdHex
    )

    is YieldBoostConfiguration.Off -> YieldBoostConfigurationParcel.Off(configuration.collatorIdHex)
}

fun YieldBoostConfiguration(parcel: YieldBoostConfigurationParcel) = when (parcel) {
    is YieldBoostConfigurationParcel.On -> YieldBoostConfiguration.On(
        threshold = parcel.threshold,
        frequencyInDays = parcel.frequencyInDays,
        collatorIdHex = parcel.collatorIdHex
    )

    is YieldBoostConfigurationParcel.Off -> YieldBoostConfiguration.Off(parcel.collatorIdHex)
}
