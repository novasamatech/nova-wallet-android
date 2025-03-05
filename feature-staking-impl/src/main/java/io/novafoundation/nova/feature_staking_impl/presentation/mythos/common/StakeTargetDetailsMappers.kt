package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common

import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.toParcel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakeTargetStakeParcelModel.Active.UserStakeInfo

fun MythosCollator.toTargetDetailsParcel(): StakeTargetDetailsParcelModel {
    val stake = if (apr != null) {
        StakeTargetStakeParcelModel.Active(
            totalStake = totalStake,
            ownStake = null,
            minimumStake = null,
            stakers = null,
            stakersCount = delegators,
            rewards = apr.inFraction.toBigDecimal(),
            isOversubscribed = false,
            userStakeInfo = UserStakeInfo(willBeRewarded = true)
        )
    } else {
        StakeTargetStakeParcelModel.Inactive
    }

    return StakeTargetDetailsParcelModel(
        accountIdHex = accountId.toHex(),
        isSlashed = false,
        stake = stake,
        identity = identity?.toParcel()
    )
}
