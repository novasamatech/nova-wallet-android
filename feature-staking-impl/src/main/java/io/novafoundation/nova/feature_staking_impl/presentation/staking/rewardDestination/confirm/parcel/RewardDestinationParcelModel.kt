package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class RewardDestinationParcelModel : Parcelable {

    @Parcelize
    object Restake : RewardDestinationParcelModel()

    @Parcelize
    class Payout(val targetAccountAddress: String) : RewardDestinationParcelModel()
}
