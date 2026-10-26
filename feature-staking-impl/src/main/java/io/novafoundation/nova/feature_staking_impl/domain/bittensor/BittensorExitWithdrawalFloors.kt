package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import java.math.BigDecimal
import java.math.BigInteger

internal fun BittensorExitPreflight.withWithdrawalFloors(
    netuid: BittensorNetuid,
    slippageFraction: BigDecimal
): BittensorExitPreflight {
    val price = withdrawalPrice ?: return this
    val slippage = BittensorSlippage(slippageFraction)
    val limit = if (netuid.isRoot()) price else slippage.limitPrice(price, BittensorSwapDirection.ALPHA_TO_TAO)
    return copy(
        withdrawalFloor = bittensorWithdrawalFloor(minStake, limit, swapFeeRate),
        sweepFloor = bittensorWithdrawalFloor(minNominatorStake, limit, BigInteger.ZERO),
        exitFloor = if (netuid.isRoot()) BigInteger.ZERO else bittensorWithdrawalFloor(BigInteger.ONE, limit, swapFeeRate)
    )
}
