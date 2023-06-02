package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AssetSearchInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val assetSourceRegistry: AssetSourceRegistry
) {

    fun buyAssetSearch(
        queryFlow: Flow<String>,
        offChainBalanceFlow: Flow<Map<FullChainAssetId, Balance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, offChainBalanceFlow) {
            it.token.configuration.buyProviders.isNotEmpty()
        }
    }

    fun sendAssetSearch(
        queryFlow: Flow<String>,
        offChainBalanceFlow: Flow<Map<FullChainAssetId, Balance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, offChainBalanceFlow) {
            val chainAsset = it.token.configuration
            assetSourceRegistry.sourceFor(chainAsset).transfers.areTransfersEnabled(chainAsset)
        }
    }

    fun searchAssetsFlow(
        queryFlow: Flow<String>,
        offChainBalanceFlow: Flow<Map<FullChainAssetId, Balance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, offChainBalanceFlow, null)
    }

    private fun searchAssetsInternalFlow(
        queryFlow: Flow<String>,
        offChainBalanceFlow: Flow<Map<FullChainAssetId, Balance>>,
        filter: (suspend (Asset) -> Boolean)?
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        var assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        if (filter != null) {
            assetsFlow = assetsFlow.map { assets ->
                assets.filter { filter(it) }
            }
        }

        return combine(
            assetsFlow,
            offChainBalanceFlow,
            queryFlow
        ) { assets, offChainBalance, query ->
            val chainsById = chainRegistry.chainsById()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, offChainBalance, chainsById)
        }
    }

    private fun List<Asset>.filterBy(query: String, chainsById: ChainsById): List<Asset> {
        return searchTokens(
            query = query,
            chainsById = chainsById,
            tokenSymbol = { it.token.configuration.symbol },
            relevantToChains = { asset, chainIds -> asset.token.configuration.chainId in chainIds }
        )
    }
}
