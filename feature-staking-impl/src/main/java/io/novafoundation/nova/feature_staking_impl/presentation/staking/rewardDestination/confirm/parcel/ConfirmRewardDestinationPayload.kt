package io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel

import android.os.Parcelable
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeParcelModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ConfirmRewardDestinationPayload(
    val fee: FeeParcelModel,
    val rewardDestination: RewardDestinationParcelModel,
) : Parcelable
