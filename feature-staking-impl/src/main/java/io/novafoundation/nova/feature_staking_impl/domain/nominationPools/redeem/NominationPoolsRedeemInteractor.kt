package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.numberOfSlashingSpans
import io.novafoundation.nova.feature_staking_api.domain.model.totalRedeemableIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.withdrawUnbonded
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.totalPointsAfterRedeemAt
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.unlockChunksFor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

interface NominationPoolsRedeemInteractor {

    fun redeemAmountFlow(poolMember: PoolMember, computationScope: CoroutineScope): Flow<Balance>

    suspend fun estimateFee(poolMember: PoolMember): Fee

    suspend fun redeem(poolMember: PoolMember): Result<RedeemConsequences>
}

class RealNominationPoolsRedeemInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: StakingRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
    private val stakingSharedState: StakingSharedState,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedComputation: StakingSharedComputation,
) : NominationPoolsRedeemInteractor {

    override fun redeemAmountFlow(poolMember: PoolMember, computationScope: CoroutineScope): Flow<Balance> {
        return flowOfAll {
            val chainId = stakingSharedState.chainId()

            combine(
                nominationPoolSharedComputation.unbondingPoolsFlow(poolMember.poolId, chainId, computationScope),
                stakingSharedComputation.activeEraFlow(chainId, computationScope)
            ) { unbondingPools, activeEra ->
                unbondingPools.unlockChunksFor(poolMember).totalRedeemableIn(activeEra)
            }
        }
    }

    override suspend fun estimateFee(poolMember: PoolMember): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                redeem(poolMember)
            }
        }
    }

    override suspend fun redeem(poolMember: PoolMember): Result<RedeemConsequences> {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()
            val activeEra = stakingRepository.getActiveEraIndex(chain.id)

            extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                redeem(poolMember)
            }.map {
                val totalAfterRedeem = poolMember.totalPointsAfterRedeemAt(activeEra)

                RedeemConsequences(willKillStash = totalAfterRedeem.value.isZero)
            }
        }
    }

    private suspend fun ExtrinsicBuilder.redeem(poolMember: PoolMember) {
        val chainId = stakingSharedState.chainId()
        val poolStash = poolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)
        val slashingSpans = stakingRepository.getSlashingSpan(chainId, poolStash).numberOfSlashingSpans()

        nominationPools.withdrawUnbonded(poolMember.accountId, slashingSpans)
    }
}
