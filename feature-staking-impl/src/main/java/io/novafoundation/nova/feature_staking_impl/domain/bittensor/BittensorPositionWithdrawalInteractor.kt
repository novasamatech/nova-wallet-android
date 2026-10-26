package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class BittensorWithdrawalRequest(
    val metaId: Long,
    val chainId: String,
    val netuid: BittensorNetuid,
    val primary: AccountIdKey,
    val slippageFraction: BigDecimal
)

data class BittensorPositionWithdrawal(
    val available: BigInteger,
    val locked: BigInteger,
    val holdDurationMillis: BigInteger,
    val canWithdraw: Boolean,
    val rootHold: BittensorRootHold? = null
)

data class BittensorRootHold(val remainingMillis: BigInteger, val intervalMillis: BigInteger)

class BittensorPositionWithdrawalInteractor @Inject constructor(
    private val source: BittensorExitRuntimeSource,
    private val accounts: AccountRepository,
    private val chains: ChainRegistry,
    private val chainState: ChainStateRepository
) {

    fun observe(request: BittensorWithdrawalRequest): Flow<BittensorPositionWithdrawal> = flowOfAll {
        val chain = chains.getChain(request.chainId)
        accounts.metaAccountFlow(request.metaId).flatMapLatest { wallet ->
            val coldkey = wallet.requireAccountIdIn(chain)
            flowOfAll { chainState.currentRemoteBlockNumberFlow(chain.id) }
                .conflate()
                .map {
                    val preflight = source.preflight(chain.id, coldkey, request.primary.value, request.netuid)
                    val blockMillis = chainState.expectedBlockTimeInMillis(chain.id)
                    preflight.positionWithdrawal(request.netuid, request.slippageFraction, blockMillis)
                }
        }
    }
}

internal fun BittensorExitPreflight.positionWithdrawal(
    netuid: BittensorNetuid,
    slippageFraction: BigDecimal,
    blockMillis: BigInteger
): BittensorPositionWithdrawal {
    // Subnet details show network availability, like iOS. The exit floor belongs to Sell's
    // Max and plan: a positive dust position is not a network lock and must not block entry.
    val maximum = if (netuid.isRoot()) {
        withWithdrawalFloors(netuid, slippageFraction).maximumAmount
    } else {
        totalStake.min(availability.available).max(BigInteger.ZERO)
    }
    return BittensorPositionWithdrawal(
        available = maximum,
        locked = (totalStake - maximum).max(BigInteger.ZERO),
        holdDurationMillis = groupRootHoldRemaining * blockMillis,
        canWithdraw = maximum.isPositive() && subnetTradingEnabled && !coldkeySwapPending && !safeModeActive,
        rootHold = rootHold(netuid, blockMillis)
    )
}

private fun BittensorExitPreflight.rootHold(netuid: BittensorNetuid, blockMillis: BigInteger): BittensorRootHold? {
    if (!netuid.isRoot() || !rootHoldEnabled || !blockMillis.isPositive()) return null
    val earliestUnlock = groupMembers.values.minOfOrNull { it.holdRemaining } ?: rootHoldRemaining
    if (!earliestUnlock.isPositive() || !rootHoldInterval.isPositive()) return null
    return BittensorRootHold(earliestUnlock * blockMillis, rootHoldInterval * blockMillis)
}
