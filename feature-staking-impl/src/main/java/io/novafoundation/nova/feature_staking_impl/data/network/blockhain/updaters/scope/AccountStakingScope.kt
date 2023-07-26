package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

class AccountStakingScope(
    private val accountRepository: AccountRepository,
    private val accountStakingDao: AccountStakingDao,
    private val sharedStakingState: StakingSharedState
) : UpdateScope<AccountStakingLocal> {

    override fun invalidationFlow(): Flow<AccountStakingLocal> {
        return combineToPair(
            sharedStakingState.assetWithChain,
            accountRepository.selectedMetaAccountFlow()
        ).flatMapLatest { (chainWithAsset, account) ->
            val (chain, chainAsset) = chainWithAsset
            val accountId = account.accountIdIn(chain) ?: return@flatMapLatest emptyFlow()

            accountStakingDao.observeDistinct(chain.id, chainAsset.id, accountId)
        }
    }
}
