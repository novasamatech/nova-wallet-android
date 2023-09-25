package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.getAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import jp.co.soramitsu.fearless_utils.hash.isPositive
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
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow) {
            it.token.configuration.buyProviders.isNotEmpty()
        }
    }

    fun sendAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        val comparator = compareByDescending(AssetGroup::groupTransferableBalanceFiat)

        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, comparator) { asset ->
            val chainAsset = asset.token.configuration
            asset.transferableInPlanks.isPositive() &&
                assetSourceRegistry.sourceFor(chainAsset)
                    .transfers.areTransfersEnabled(chainAsset)
        }
    }

    fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    private fun searchAssetsInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<AssetGroup> = getAssetGroupBaseComparator(),
        filter: (suspend (Asset) -> Boolean)?,
    ): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        var assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        if (filter != null) {
            assetsFlow = assetsFlow.map { assets ->
                assets.filter { filter(it) }
            }
        }

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.chainsById()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, externalBalances, chainsById, assetGroupComparator)
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
