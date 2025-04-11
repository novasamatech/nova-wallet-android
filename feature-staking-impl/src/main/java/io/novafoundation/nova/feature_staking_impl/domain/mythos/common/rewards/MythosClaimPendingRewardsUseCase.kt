package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.rewards

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdKeyOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.claimRewards
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import javax.inject.Inject

interface MythosClaimPendingRewardsUseCase {

    context(ExtrinsicBuilder)
    suspend fun claimPendingRewards(chain: Chain)
}

@FeatureScope
class RealMythosClaimPendingRewardsUseCase @Inject constructor(
    private val userStakeRepository: MythosUserStakeRepository,
    private val accountRepository: AccountRepository,
) : MythosClaimPendingRewardsUseCase {

    context(ExtrinsicBuilder)
    override suspend fun claimPendingRewards(chain: Chain) {
        val accountId = accountRepository.requireIdKeyOfSelectedMetaAccountIn(chain)
        val hasPendingRewards = userStakeRepository.shouldClaimRewards(chain.id, accountId)
        if (hasPendingRewards) {
            collatorStaking.claimRewards()
        }
    }
}
