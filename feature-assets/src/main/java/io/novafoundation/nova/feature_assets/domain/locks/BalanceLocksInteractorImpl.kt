package io.novafoundation.nova.feature_assets.domain.locks

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.Flow

class BalanceLocksInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val balanceHoldsRepository: BalanceHoldsRepository,
    private val accountRepository: AccountRepository,
) : BalanceLocksInteractor {

    override fun balanceLocksFlow(chainId: ChainId, chainAssetId: Int): Flow<List<BalanceLock>> {
        return flowOfAll {
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val selectedAccount = accountRepository.getSelectedMetaAccount()
            balanceLocksRepository.observeBalanceLocks(selectedAccount.id, chain, chainAsset)
        }
    }

    override fun balanceHoldsFlow(chainId: ChainId, chainAssetId: Int): Flow<List<BalanceHold>> {
        return flowOfAll {
            val chainAsset = chainRegistry.asset(chainId, chainAssetId)
            val selectedAccount = accountRepository.getSelectedMetaAccount()
            balanceHoldsRepository.observeBalanceHolds(selectedAccount.id, chainAsset)
        }
    }
}
