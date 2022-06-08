package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetDetailsParcelModel

suspend fun StakeTargetDetailsPayload.Companion.parachain(
    stakeTarget: StakeTargetDetailsParcelModel,
    collatorsUseCase: CollatorsUseCase,
) = StakeTargetDetailsPayload(
    stakeTarget = stakeTarget,
    displayConfig = StakeTargetDetailsPayload.DisplayConfig(
        rewardSuffix = RewardSuffix.APR,
        rewardedStakersPerStakeTarget = collatorsUseCase.maxRewardedDelegatorsPerCollator(),
        titleRes = R.string.staking_parachain_collator_info,
        stakersLabelRes = R.string.staking_parachain_delegators,
        oversubscribedWarningText = R.string.staking_parachain_collator_details_oversubscribed
    )
)
