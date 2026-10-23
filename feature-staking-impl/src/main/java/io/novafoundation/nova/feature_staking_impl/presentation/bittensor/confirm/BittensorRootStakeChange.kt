package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import java.math.BigInteger

/**
 * The whole root stake before and after adding [staked] to [validator], as iOS reports it on review and in the result
 * (`group.totalAlpha`, `hotkeyCountAfterOperation`).
 */
class BittensorRootStakeChange(val before: BigInteger, val after: BigInteger, val validatorCount: Int)

internal fun bittensorRootStakeChange(
    positions: List<BittensorRootPosition>,
    validator: AccountIdKey,
    staked: BigInteger
): BittensorRootStakeChange {
    val active = positions.filter { it.stakeInPlanks.signum() > 0 }
    val before = active.fold(BigInteger.ZERO) { total, position -> total + position.stakeInPlanks }
    val validators = active.mapTo(mutableSetOf()) { it.validatorHotkey } + validator
    return BittensorRootStakeChange(before, before + staked, validators.size)
}

/** As on iOS, "Stake after" belongs to adding stake and is hidden while the positions are not fresh. */
internal fun bittensorShowsRootStakeAfter(existingPosition: Boolean, status: BittensorDataStatus): Boolean {
    return existingPosition && status is BittensorDataStatus.Fresh
}

/**
 * iOS done sheet: a new position lists Validator + Network fee; adding to one reports the stake after
 * ("with <validator>" only while a single validator holds the root stake) and lists Stake after + Network fee.
 */
internal fun bittensorRootStakeSuccess(
    resources: ResourceManager,
    existingPosition: Boolean,
    amount: String,
    stakeAfter: String,
    validatorName: String,
    validatorCount: Int,
    fee: BittensorFeeUi,
    actions: List<BittensorResultAction>
): BittensorStakeResultUi.Success {
    if (!existingPosition) {
        return BittensorStakeResultUi.Success(
            title = resources.getString(R.string.bittensor_staking_success_title, amount),
            description = resources.getString(R.string.bittensor_staking_success_description),
            validator = validatorName,
            fee = fee,
            actions = actions
        )
    }

    val description = if (validatorCount == 1) {
        resources.getString(R.string.bittensor_staking_more_success_description_with, stakeAfter, validatorName)
    } else {
        resources.getString(R.string.bittensor_staking_more_success_description, stakeAfter)
    }
    return BittensorStakeResultUi.Success(
        title = resources.getString(R.string.bittensor_staking_more_success_title, amount),
        description = description,
        validator = null,
        fee = fee,
        stakeAfter = stakeAfter,
        actions = actions
    )
}
