package io.novafoundation.nova.feature_crowdloan_impl.data.network.updater

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.AssetBalanceScope.ScopeValue
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AssetBalanceScopeFactory(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
) {

    fun create(chain: Chain, asset: Chain.Asset): AssetBalanceScope {
        return RealAssetBalanceScope(chain, asset, walletRepository, accountRepository)
    }
}

class RealAssetBalanceScope(
    private val chain: Chain,
    private val asset: Chain.Asset,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
) : AssetBalanceScope {

    override fun invalidationFlow(): Flow<ScopeValue> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            walletRepository.assetFlow(metaAccount.id, asset).map { asset ->
                ScopeValue(metaAccount, asset)
            }
        }
            .distinctUntilChanged { old, new ->
                old.asset.totalInPlanks == new.asset.totalInPlanks &&
                    old.metaAccount.id == new.metaAccount.id &&
                    old.metaAccount.accountIdIn(chain).contentEquals(new.metaAccount.accountIdIn(chain))
            }
    }
}
