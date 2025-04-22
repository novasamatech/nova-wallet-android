package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.unbond

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.unbond
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.delegatedStake.DelegatedStakeMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getParticipatingBondedPoolState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.participatingBondedPoolStateFlow
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.pointsOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface NominationPoolsUnbondInteractor {

    fun poolMemberStateFlow(computationScope: CoroutineScope): Flow<PoolMemberState>

    suspend fun estimateFee(poolMember: PoolMember, amount: Balance): Fee

    suspend fun unbond(poolMember: PoolMember, amount: Balance): Result<ExtrinsicSubmission>
}

class RealNominationPoolsUnbondInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val migrationUseCase: DelegatedStakeMigrationUseCase
) : NominationPoolsUnbondInteractor {

    override fun poolMemberStateFlow(computationScope: CoroutineScope): Flow<PoolMemberState> {
        return poolMemberUseCase.currentPoolMemberFlow()
            .filterNotNull()
            .flatMapLatest { poolMember ->
                val poolId = poolMember.poolId
                val chainId = stakingSharedState.chainId()
                val stash = poolAccountDerivation.bondedAccountOf(poolId, chainId)

                nominationPoolSharedComputation.participatingBondedPoolStateFlow(stash, poolId, chainId, computationScope).map { bondedPoolState ->
                    PoolMemberState(bondedPoolState, poolMember)
                }
            }
    }

    override suspend fun estimateFee(poolMember: PoolMember, amount: Balance): Fee {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()

            extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
                unbond(poolMember, amount, chain.id)
            }
        }
    }

    override suspend fun unbond(poolMember: PoolMember, amount: Balance): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()

            extrinsicService.submitExtrinsic(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                unbond(poolMember, amount, chain.id)
            }
        }
    }

    private suspend fun ExtrinsicBuilder.unbond(poolMember: PoolMember, amount: Balance, chainId: ChainId) {
        migrationUseCase.migrateToDelegatedStakeIfNeeded()

        val poolAccount = poolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)

        val bondedPoolState = nominationPoolSharedComputation.getParticipatingBondedPoolState(poolAccount, poolMember.poolId, chainId)
        val unbondPoints = bondedPoolState.pointsOf(amount).coerceAtMost(poolMember.points)

        nominationPools.unbond(poolMember.accountId, unbondPoints)
    }
}
