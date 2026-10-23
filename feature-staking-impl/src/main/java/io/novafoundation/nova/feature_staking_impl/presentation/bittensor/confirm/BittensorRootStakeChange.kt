package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
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
