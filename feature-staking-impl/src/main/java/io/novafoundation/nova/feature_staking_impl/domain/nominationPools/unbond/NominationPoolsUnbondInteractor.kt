package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.NominationPoolsCalls
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.unbond
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getParticipatingBondedPoolState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.participatingBondedPoolStateFlow
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.BondedPoolState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.pointsOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface NominationPoolsUnbondInteractor {

    fun bondedPoolStateFlow(poolId: PoolId, computationScope: CoroutineScope): Flow<BondedPoolState>

    suspend fun estimateFee(poolMember: PoolMember, amount: Balance): Balance

    suspend fun unbond(poolMember: PoolMember, amount: Balance): Result<String>
}

class RealNominationPoolsUnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val poolAccountDerivation: PoolAccountDerivation,
) : NominationPoolsUnbondInteractor {


    override fun bondedPoolStateFlow(poolId: PoolId, computationScope: CoroutineScope): Flow<BondedPoolState> {
        return flowOfAll {
            val chainId = stakingSharedState.chainId()
            val stash = poolAccountDerivation.bondedAccountOf(poolId, chainId)

            nominationPoolSharedComputation.participatingBondedPoolStateFlow(stash, poolId, chainId, computationScope)
        }
    }

    override suspend fun estimateFee(poolMember: PoolMember, amount: Balance): Balance {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()

            extrinsicService.estimateFee(chain) {
                nominationPools.unbond(poolMember, amount, chain.id)
            }
        }
    }

    override suspend fun unbond(poolMember: PoolMember, amount: Balance): Result<String> {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()

            extrinsicService.submitExtrinsicWithSelectedWallet(stakingSharedState.chain()) {
                nominationPools.unbond(poolMember, amount, chain.id)
            }
        }
    }

    private suspend fun NominationPoolsCalls.unbond(poolMember: PoolMember, amount: Balance, chainId: ChainId) {
        val poolAccount = poolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)

        val bondedPoolState = nominationPoolSharedComputation.getParticipatingBondedPoolState(poolAccount, poolMember.poolId, chainId)
        val unbondPoints = bondedPoolState.pointsOf(amount)

        unbond(poolMember.accountId, unbondPoints)
    }
}
