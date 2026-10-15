package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class BittensorPortfolio(
    val metaId: Long,
    val chain: Chain,
    val asset: Asset,
    val positions: BittensorPortfolioPositions?,
    val dataStatus: BittensorDataStatus
)

data class BittensorStakePositions(
    val root: List<BittensorRootPosition>,
    val subnets: List<BittensorSubnetPosition>
)

data class BittensorPortfolioPositions(
    val root: List<BittensorRootPosition>,
    val subnets: List<BittensorSubnetPosition>,
    val subnetPrices: Map<BittensorSubnetId, BigInteger>
) {

    val rootStake: BigInteger = root.fold(BigInteger.ZERO) { total, position -> total + position.stakeInPlanks }

    private val alphaBySubnet = subnets.groupBy { it.subnetId }.mapValues { (_, positions) ->
        positions.fold(BigInteger.ZERO) { total, position -> total + position.alphaInPlanks }
    }

    fun subnetStake(subnetId: BittensorSubnetId): BigInteger? {
        val price = subnetPrices[subnetId] ?: return null
        val alpha = alphaBySubnet[subnetId] ?: BigInteger.ZERO
        return alpha * price / ALPHA_PRECISION
    }

    // Never report a partial portfolio as the user's total when a subnet price is unavailable.
    val totalStake: BigInteger?
        get() {
            var total = rootStake
            for (subnet in alphaBySubnet.keys) {
                total += subnetStake(subnet) ?: return null
            }
            return total
        }

    private companion object {
        val ALPHA_PRECISION: BigInteger = BigInteger.TEN.pow(9)
    }
}
