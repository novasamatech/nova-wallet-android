package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

internal fun BittensorStakePositions?.heldSubnets(): Set<BittensorNetuid.Subnet> = this?.subnets.orEmpty()
    .filter { it.alphaInPlanks.isPositive() }
    .map { it.subnetId }
    .toSet()

internal fun mapBittensorPortfolio(
    positions: BittensorStakePositions,
    prices: Map<BittensorNetuid.Subnet, BigInteger>
): BittensorPortfolioPositions = BittensorPortfolioPositions(positions.root, positions.subnets, prices)
