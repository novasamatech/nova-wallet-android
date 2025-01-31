package io.novafoundation.nova.feature_staking_impl.presentation.validators.details

import android.os.Parcelable
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetDetailsParcelModel
import kotlinx.parcelize.Parcelize

@Parcelize
class StakeTargetDetailsPayload(
    val stakeTarget: StakeTargetDetailsParcelModel,
    val displayConfig: DisplayConfig
) : Parcelable {

    companion object

    @Parcelize
    class DisplayConfig(
        val rewardSuffix: RewardSuffix,
        val rewardedStakersPerStakeTarget: Int?,
        @StringRes val titleRes: Int,
        @StringRes val stakersLabelRes: Int,
        @StringRes val oversubscribedWarningText: Int,
    ) : Parcelable
}

suspend fun StakeTargetDetailsPayload.Companion.relaychain(
    stakeTarget: StakeTargetDetailsParcelModel,
    stakingInteractor: StakingInteractor,
): StakeTargetDetailsPayload {
    return StakeTargetDetailsPayload(
        stakeTarget = stakeTarget,
        displayConfig = StakeTargetDetailsPayload.DisplayConfig(
            rewardSuffix = RewardSuffix.APY,
            rewardedStakersPerStakeTarget = stakingInteractor.maxRewardedNominators(),
            titleRes = R.string.staking_validator_info_title,
            stakersLabelRes = R.string.staking_validator_nominators,
            oversubscribedWarningText = R.string.staking_validator_my_oversubscribed_message
        )
    )
}
