package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

interface NominationPoolMemberUseCase {

    fun currentPoolMemberFlow(): Flow<PoolMember?>
}

class RealNominationPoolMemberUseCase(
    private val accountRepository: AccountRepository,
    private val stakingSharedState: StakingSharedState,
    private val nominationPoolMembersRepository: NominationPoolMembersRepository,
) : NominationPoolMemberUseCase {

    override fun currentPoolMemberFlow(): Flow<PoolMember?> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { selectedMetaAccount ->
            val chain = stakingSharedState.chain()

            val accountId = selectedMetaAccount.accountIdIn(chain) ?: return@flatMapLatest flowOf(null)

            nominationPoolMembersRepository.observePoolMember(chain.id, accountId)
        }
    }
}
