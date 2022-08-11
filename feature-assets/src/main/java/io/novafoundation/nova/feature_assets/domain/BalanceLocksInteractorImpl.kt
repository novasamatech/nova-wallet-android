package io.novafoundation.nova.feature_assets.domain

import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLocks
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.flow.*

class BalanceLocksInteractorImpl(
    private val updateSystem: UpdateSystem,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : BalanceLocksInteractor {

    override fun balanceLocksFlow(chainId: ChainId, chainAssetId: Int): Flow<BalanceLocks?> {
        return flow {
            val (chain, chainAsset) = chainRegistry.chainWithAsset(chainId, chainAssetId)
            val assetSource = assetSourceRegistry.sourceFor(chainAsset)
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val accountId = metaAccount.accountIdIn(chain) ?: throw NoSuchElementException()
            emitAll(assetSource.balance.queryBalanceLocks(chain, chainAsset, accountId))
        }
    }

    override fun runBalanceLocksUpdate(): Flow<Updater.SideEffect> {
        return updateSystem.start()
    }
}
