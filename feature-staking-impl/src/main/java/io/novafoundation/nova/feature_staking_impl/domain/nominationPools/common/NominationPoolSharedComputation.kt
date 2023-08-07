package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.BondedPoolState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext

class NominationPoolSharedComputation(
    private val computationalCache: ComputationalCache,
    private val nominationPoolMemberUseCase: NominationPoolMemberUseCase,
    private val nominationPoolStateRepository: NominationPoolStateRepository,
    private val nominationPoolUnbondRepository: NominationPoolUnbondRepository,
) {

    fun currentPoolMemberFlow(chain: Chain, scope: CoroutineScope): Flow<PoolMember?> {
        val key = "POOL_MEMBER:${chain.id}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolMemberUseCase.currentPoolMemberFlow(chain)
        }
    }

    fun participatingBondedPoolFlow(poolId: PoolId, chainId: ChainId, scope: CoroutineScope): Flow<BondedPool> {
        val key = "BONDED_POOL:$chainId:${poolId.value}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolStateRepository.observeParticipatingBondedPool(poolId, chainId)
        }
    }

    fun unbondingPoolsFlow(poolId: PoolId, chainId: ChainId, scope: CoroutineScope): Flow<UnbondingPools?> {
        val key = "UNBONDING_POOLS:$chainId:${poolId.value}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolUnbondRepository.unbondingPoolsFlow(poolId, chainId)
        }
    }

    fun participatingPoolNominationsFlow(
        poolStash: AccountId,
        poolId: PoolId,
        chainId: ChainId,
        scope: CoroutineScope
    ): Flow<Nominations?> {
        val key = "POOL_NOMINATION:$chainId:${poolId.value}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolStateRepository.observeParticipatingPoolNominations(poolStash, chainId)
        }
    }

    fun participatingBondedBalanceFlow(
        poolStash: AccountId,
        poolId: PoolId,
        chainId: ChainId,
        scope: CoroutineScope
    ): Flow<Balance> {
        val key = "POOL_BONDED_BALANCE:$chainId:${poolId.value}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolStateRepository.observeParticipatingBondedBalance(poolStash, chainId)
        }
    }
}

fun NominationPoolSharedComputation.participatingBondedPoolStateFlow(
    poolStash: AccountId,
    poolId: PoolId,
    chainId: ChainId,
    scope: CoroutineScope
): Flow<BondedPoolState> = combine(
    participatingBondedPoolFlow(poolId, chainId, scope),
    participatingBondedBalanceFlow(poolStash, poolId, chainId, scope),
    ::BondedPoolState
)

suspend fun NominationPoolSharedComputation.getParticipatingBondedPoolState(
    poolStash: AccountId,
    poolId: PoolId,
    chainId: ChainId
): BondedPoolState = participatingBondedPoolStateFlow(poolStash, poolId, chainId, CoroutineScope(coroutineContext)).first()
