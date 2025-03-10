package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.bondMore

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.NominationPoolBondExtraSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.bondExtra
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.delegatedStake.DelegatedStakeMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.participatingBondedPoolStateFlow
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface NominationPoolsBondMoreInteractor {

    suspend fun estimateFee(bondMoreAmount: Balance): Fee

    suspend fun bondMore(bondMoreAmount: Balance): Result<ExtrinsicSubmission>

    suspend fun stakeAmount(
        poolMember: PoolMember,
        chainId: ChainId,
        sharedComputationScope: CoroutineScope
    ): Flow<Balance>
}

class RealNominationPoolsBondMoreInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
    private val migrationUseCase: DelegatedStakeMigrationUseCase,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val poolAccountDerivation: PoolAccountDerivation,
) : NominationPoolsBondMoreInteractor {

    override suspend fun estimateFee(bondMoreAmount: Balance): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                bondExtra(bondMoreAmount)
            }
        }
    }

    override suspend fun bondMore(bondMoreAmount: Balance): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                bondExtra(bondMoreAmount)
            }
        }
    }

    override suspend fun stakeAmount(poolMember: PoolMember, chainId: ChainId, sharedComputationScope: CoroutineScope): Flow<Balance> {
        val poolStash = poolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)

        return nominationPoolSharedComputation.participatingBondedPoolStateFlow(poolStash, poolMember.poolId, chainId, sharedComputationScope)
            .map { it.amountOf(poolMember.points) }
    }

    private suspend fun ExtrinsicBuilder.bondExtra(amount: Balance) {
        migrationUseCase.migrateToDelegatedStakeIfNeeded()

        nominationPools.bondExtra(NominationPoolBondExtraSource.FreeBalance(amount))
    }
}
