package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.activeBalance
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.BondedPoolState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.coroutines.coroutineContext

class NominationPoolSharedComputation(
    private val computationalCache: ComputationalCache,
    private val nominationPoolMemberUseCase: NominationPoolMemberUseCase,
    private val nominationPoolStateRepository: NominationPoolStateRepository,
    private val nominationPoolUnbondRepository: NominationPoolUnbondRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val nominationPoolRewardCalculatorFactory: NominationPoolRewardCalculatorFactory,
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

    fun participatingBondedPoolLedgerFlow(
        poolStash: AccountId,
        poolId: PoolId,
        chainId: ChainId,
        scope: CoroutineScope
    ): Flow<StakingLedger?> {
        val key = "POOL_BONDED_LEDGER:$chainId:${poolId.value}"

        return computationalCache.useSharedFlow(key, scope) {
            nominationPoolStateRepository.observeParticipatingPoolLedger(poolStash, chainId)
        }
    }

    suspend fun participatingBondedPoolLedger(
        poolId: PoolId,
        chainId: ChainId,
        scope: CoroutineScope
    ): StakingLedger? {
        val poolStash = poolAccountDerivation.bondedAccountOf(poolId, chainId)

        return participatingBondedPoolLedgerFlow(poolStash, poolId, chainId, scope).first()
    }

    suspend fun poolRewardCalculator(
        stakingOption: StakingOption,
        scope: CoroutineScope
    ): NominationPoolRewardCalculator {
        val key = "NOMINATION_POOLS_REWARD_CALCULATOR:${stakingOption.chain.id}"

        return computationalCache.useCache(key, scope) {
            nominationPoolRewardCalculatorFactory.create(stakingOption, scope)
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
    participatingBondedPoolLedgerFlow(poolStash, poolId, chainId, scope).map { it.activeBalance() },
    ::BondedPoolState
)

suspend fun NominationPoolSharedComputation.getParticipatingBondedPoolState(
    poolStash: AccountId,
    poolId: PoolId,
    chainId: ChainId
): BondedPoolState = participatingBondedPoolStateFlow(poolStash, poolId, chainId, CoroutineScope(coroutineContext)).first()
