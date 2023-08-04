package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isNonPositive
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.state.chainAndAsset

interface NominationPoolHintsUseCase {

    suspend fun rewardsWillBeClaimedHint(): String?
}

class RealNominationPoolHintsUseCase(
    private val stakingSharedState: StakingSharedState,
    private val poolMembersRepository: NominationPoolMembersRepository,
    private val accountRepository: AccountRepository,
    private val resourceManager: ResourceManager,
): NominationPoolHintsUseCase {

    override suspend fun rewardsWillBeClaimedHint(): String? = runCatching {
        val account = accountRepository.getSelectedMetaAccount()
        val (chain, chainAsset) = stakingSharedState.chainAndAsset()

        val accountId = account.requireAccountIdIn(chain)

        val pendingRewards = poolMembersRepository.getPendingRewards(accountId, chain.id)

        if (pendingRewards.isNonPositive) return@runCatching null

        val amountFormatted = pendingRewards.formatPlanks(chainAsset)

        resourceManager.getString(R.string.nomination_pools_pending_claim_hint_format, amountFormatted)
    }.getOrNull()
}
