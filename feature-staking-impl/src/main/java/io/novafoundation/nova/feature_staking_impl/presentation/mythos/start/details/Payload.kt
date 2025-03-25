package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.toTargetDetailsParcel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

fun StakeTargetDetailsPayload.Companion.mythos(collator: MythosCollator) = StakeTargetDetailsPayload(
    stakeTarget = collator.toTargetDetailsParcel(),
    displayConfig = StakeTargetDetailsPayload.DisplayConfig(
        rewardSuffix = RewardSuffix.APR,
        rewardedStakersPerStakeTarget = null,
        titleRes = R.string.staking_parachain_collator_info,
        stakersLabelRes = R.string.staking_parachain_delegators,
        oversubscribedWarningText = R.string.staking_parachain_collator_details_oversubscribed
    )
)
