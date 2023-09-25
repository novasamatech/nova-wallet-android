package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.NominationPoolBondExtraSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.bondExtra
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.claimPayout
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.calls.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

interface NominationPoolsClaimRewardsInteractor {

    fun pendingRewardsFlow(): Flow<Balance>

    suspend fun estimateFee(shouldRestake: Boolean): Balance

    suspend fun claimRewards(shouldRestake: Boolean): Result<String>
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

    override suspend fun estimateFee(shouldRestake: Boolean): Balance {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingSharedState.chain()) {
                claimRewards(shouldRestake)
            }
        }
    }

    override suspend fun claimRewards(shouldRestake: Boolean): Result<String> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsicWithSelectedWallet(stakingSharedState.chain()) {
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
