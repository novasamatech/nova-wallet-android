package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.NominationPoolBondExtraSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.bondExtra
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.claimPayout
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

interface NominationPoolsClaimRewardsInteractor {

    fun pendingRewardsFlow(): Flow<Balance>

    suspend fun estimateFee(shouldRestake: Boolean): Fee

    suspend fun claimRewards(shouldRestake: Boolean): Result<ExtrinsicSubmission>
}

class RealNominationPoolsClaimRewardsInteractor(
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val poolMembersRepository: NominationPoolMembersRepository,
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
) : NominationPoolsClaimRewardsInteractor {

    override fun pendingRewardsFlow(): Flow<Balance> {
        return poolMemberUseCase.currentPoolMemberFlow()
            .filterNotNull()
            .distinctUntilChangedBy { it.lastRecordedRewardCounter }
            .mapLatest { poolMember ->
                poolMembersRepository.getPendingRewards(poolMember.accountId, stakingSharedState.chainId())
            }
    }

    override suspend fun estimateFee(shouldRestake: Boolean): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                claimRewards(shouldRestake)
            }
        }
    }

    override suspend fun claimRewards(shouldRestake: Boolean): Result<ExtrinsicSubmission> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stakingSharedState.chain(), TransactionOrigin.SelectedWallet) {
                claimRewards(shouldRestake)
            }
        }
    }

    private fun ExtrinsicBuilder.claimRewards(shouldRestake: Boolean) {
        if (shouldRestake) {
            nominationPools.bondExtra(NominationPoolBondExtraSource.Rewards)
        } else {
            nominationPools.claimPayout()
        }
    }
}
