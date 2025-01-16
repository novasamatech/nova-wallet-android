package io.novafoundation.nova.feature_staking_impl.domain.mythos.common

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.UserStakeRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

interface MythosUserStakeUseCase {

    context(ComputationalScope)
    fun currentUserStakeInfo(chain: Chain): Flow<UserStakeInfo>
}

@FeatureScope
class RealMythosUserStakeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userStakeRepository: UserStakeRepository
): MythosUserStakeUseCase {

    context(ComputationalScope)
    override fun currentUserStakeInfo(chain: Chain): Flow<UserStakeInfo> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { selectedMetaAccount ->
            val accountId = selectedMetaAccount.accountIdIn(chain) ?: return@flatMapLatest emptyFlow()

            userStakeRepository.userStakeOrDefaultFlow(chain.id , accountId)
        }
    }
}
