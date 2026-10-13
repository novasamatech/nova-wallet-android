package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class BittensorPortfolio(
    val metaId: Long,
    val chain: Chain,
    val coldkey: AccountIdKey,
    val asset: Asset,
    val positions: BittensorPortfolioPositions?,
    val dataStatus: BittensorDataStatus
)

data class BittensorPortfolioPositions(
    val root: List<BittensorRootPosition>,
    val subnets: List<BittensorSubnetPosition>,
    val subnetPrices: Map<BittensorSubnetId, BigInteger>
) {

    val rootStake: BigInteger
        get() = root.fold(BigInteger.ZERO) { total, position -> total + position.stakeInPlanks }

    fun subnetStake(subnetId: BittensorSubnetId): BigInteger? {
        val price = subnetPrices[subnetId] ?: return null
        val alpha = subnets.filter { it.subnetId == subnetId }.fold(BigInteger.ZERO) { total, position -> total + position.alphaInPlanks }
        return alpha * price / ALPHA_PRECISION
    }

    // Never report a partial portfolio as the user's total when a subnet price is unavailable.
    val totalStake: BigInteger?
        get() {
            var total = rootStake
            for (subnet in subnets.map { it.subnetId }.distinct()) {
                total += subnetStake(subnet) ?: return null
            }
            return total
        }

    private companion object {
        val ALPHA_PRECISION: BigInteger = BigInteger.TEN.pow(9)
    }
}
