package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

interface ArbitraryAssetUseCase {

    fun assetFlow(chainId: ChainId, assetId: ChainAssetId): Flow<Asset>

    fun assetFlow(chainAsset: Chain.Asset): Flow<Asset>

    suspend fun getAsset(chainAsset: Chain.Asset): Asset?
}

class RealArbitraryAssetUseCase(
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry
) : ArbitraryAssetUseCase {

    override fun assetFlow(chainId: ChainId, assetId: ChainAssetId): Flow<Asset> {
        return flowOfAll {
            val chainAsset = chainRegistry.asset(chainId, assetId)

            assetFlow(chainAsset)
        }
    }

    override fun assetFlow(chainAsset: Chain.Asset): Flow<Asset> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            walletRepository.assetFlow(metaAccount.id, chainAsset)
        }
    }

    override suspend fun getAsset(chainAsset: Chain.Asset): Asset? {
        val account = accountRepository.getSelectedMetaAccount()
        return walletRepository.getAsset(account.id, chainAsset)
    }
}
