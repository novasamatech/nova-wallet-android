package io.novafoundation.nova.feature_assets.domain.swap

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.applyFilter
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchUseCase
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.totalWithOffChain
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapAssetRole
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import io.novasama.substrate_sdk_android.hash.isPositive

class SwapTokenSelectionInteractor(
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry,
    private val assetSearchUseCase: AssetSearchUseCase,
    private val swapService: SwapService,
    private val filterFactory: SwapTokenSelectionFilterFactory
) {

    fun observe(metaAccountId: Long, opposite: FullChainAssetId?, selectingPay: Boolean, scope: CoroutineScope): Flow<SwapTokenSelectionData> {
        val selectionAssetsFlow = observeAvailableCatalogue(scope, selectingPay)
            .flatMapLatest { catalogue -> observeWalletAssets(metaAccountId, catalogue) }
        val compatibleAssetIdsFlow = observeCompatibleAssetIds(opposite, selectingPay, metaAccountId, scope)

        return combine(
            selectionAssetsFlow,
            compatibleAssetIdsFlow
        ) { selectionAssets, compatibleAssetIds ->
            mapSelectionData(selectionAssets, compatibleAssetIds, opposite)
        }
    }

    fun groups(data: SwapTokenSelectionData, query: String, networks: Set<ChainId>): Map<SwapTokenSection, List<AssetWithNetwork>> {
        val chainsById = ChainsById(data.chains.associateBy { it.id })
        val matchingAssets = filterAssets(data.assets, query, networks, chainsById)
        val sortedTokens = matchingAssets
            .mapNotNull { asset -> mapAssetWithNetwork(asset, chainsById) }
            .sortedWith(getTokenAssetBaseComparator { it.balanceWithOffChain.transferable.fiat })

        return groupBySection(sortedTokens, data)
    }

    fun asset(data: SwapTokenSelectionData, id: FullChainAssetId): Chain.Asset? =
        data.assets.firstOrNull { it.token.configuration.fullId == id }?.token?.configuration

    fun networkChips(chains: List<Chain>, savedNetworks: Set<ChainId>): List<Chain> {
        val defaultNetworks = listOf(
            ChainGeneses.POLKADOT_ASSET_HUB,
            ChainGeneses.KUSAMA_ASSET_HUB,
            ChainGeneses.HYDRA_DX,
            ChainGeneses.ASTAR,
            BIFROST_POLKADOT
        )
        val chainsById = chains.associateBy { it.id }
        val savedChains = chains.filter { it.id in savedNetworks }.sortedWith(Chain.defaultComparator())
        return (savedChains + defaultNetworks.mapNotNull(chainsById::get)).distinctBy { it.id }
    }

    suspend fun shouldClearOpposite(
        selected: FullChainAssetId,
        opposite: FullChainAssetId?,
        selectingPay: Boolean,
        metaAccountId: Long,
        scope: CoroutineScope
    ): Boolean = withContext(Dispatchers.Default) {
        if (opposite == null) return@withContext false

        val payAssetId = if (selectingPay) selected else opposite
        val receiveAssetId = if (selectingPay) opposite else selected
        !swapService.canSwap(payAssetId, receiveAssetId, metaAccountId, scope)
    }

    private fun observeAvailableCatalogue(scope: CoroutineScope, selectingPay: Boolean): Flow<SwapSelectionCatalogue> {
        val role = if (selectingPay) SwapAssetRole.PAY else SwapAssetRole.RECEIVE
        val supportedAssetIdsFlow = flowOfAll { swapService.assetsAvailableForSwap(scope, role) }

        return combine(
            chainRegistry.enabledChainsFlow(),
            supportedAssetIdsFlow
        ) { enabledChains, supportedAssetIds ->
            mapCatalogue(enabledChains, supportedAssetIds)
        }
    }

    private fun mapCatalogue(enabledChains: List<Chain>, supportedAssetIds: Set<FullChainAssetId>): SwapSelectionCatalogue {
        val chainFilter = filterFactory.createChainFilter(enabledChains)
        val availableChains = enabledChains.applyFilter(chainFilter)
        val assetConfigurations = availableChains
            .flatMap { it.assets }
            .filter { it.fullId in supportedAssetIds }

        return SwapSelectionCatalogue(availableChains, assetConfigurations)
    }

    private fun observeWalletAssets(metaAccountId: Long, catalogue: SwapSelectionCatalogue): Flow<SwapSelectionAssets> {
        val catalogueAssetsFlow = walletRepository.supportedAssetsFlow(metaAccountId, catalogue.assetConfigurations)
        val syncedAssetsFlow = walletRepository.syncedAssetsFlow(metaAccountId)

        return combine(
            catalogueAssetsFlow,
            syncedAssetsFlow
        ) { catalogueAssets, syncedAssets ->
            SwapSelectionAssets(catalogue.chains, mergeSyncedAssets(catalogueAssets, syncedAssets))
        }
    }

    private fun mergeSyncedAssets(catalogueAssets: List<Asset>, syncedAssets: List<Asset>): List<Asset> {
        val syncedAssetsById = syncedAssets.associateBy { it.token.configuration.fullId }

        return catalogueAssets.map { catalogueAsset ->
            syncedAssetsById[catalogueAsset.token.configuration.fullId] ?: catalogueAsset
        }
    }

    private fun observeCompatibleAssetIds(
        oppositeAssetId: FullChainAssetId?,
        selectingPay: Boolean,
        metaAccountId: Long,
        scope: CoroutineScope
    ): Flow<Set<FullChainAssetId>> {
        return flowOfAll {
            if (oppositeAssetId == null) {
                flowOf(emptySet())
            } else {
                swapService.availableAssetsForSelection(oppositeAssetId, selectingPay, metaAccountId, scope)
            }
        }
    }

    private fun mapSelectionData(
        selectionAssets: SwapSelectionAssets,
        compatibleAssetIds: Set<FullChainAssetId>,
        oppositeAssetId: FullChainAssetId?
    ): SwapTokenSelectionData {
        val oppositeAssetFilter = filterFactory.createOppositeAssetFilter(oppositeAssetId)
        val selectableAssets = selectionAssets.assets.applyFilter(oppositeAssetFilter)
        val selectableChainIds = selectableAssets.map { it.token.configuration.chainId }.toSet()

        return SwapTokenSelectionData(
            assets = selectableAssets,
            chains = selectionAssets.chains.filter { it.id in selectableChainIds },
            compatible = compatibleAssetIds,
            hasOpposite = oppositeAssetId != null,
            oppositeSymbol = findOppositeSymbol(selectionAssets.chains, oppositeAssetId)
        )
    }

    private fun findOppositeSymbol(chains: List<Chain>, oppositeAssetId: FullChainAssetId?): String? {
        if (oppositeAssetId == null) return null

        val oppositeChain = chains.firstOrNull { it.id == oppositeAssetId.chainId }
        val oppositeAsset = oppositeChain?.assets?.firstOrNull { it.id == oppositeAssetId.assetId }

        return oppositeAsset?.symbol?.value
    }

    private fun filterAssets(assets: List<Asset>, query: String, networks: Set<ChainId>, chainsById: ChainsById): List<Asset> {
        val networkFilter = filterFactory.createAssetNetworkFilter(networks)

        return assetSearchUseCase.filterAssetsByQuery(query.trim(), assets, chainsById)
            .applyFilter(networkFilter)
    }

    private fun mapAssetWithNetwork(asset: Asset, chainsById: ChainsById): AssetWithNetwork? {
        val chain = chainsById[asset.token.configuration.chainId] ?: return null

        return AssetWithNetwork(chain, asset, asset.totalWithOffChain(emptyMap()))
    }

    private fun groupBySection(tokens: List<AssetWithNetwork>, data: SwapTokenSelectionData): Map<SwapTokenSection, List<AssetWithNetwork>> {
        val tokensBySection = tokens.groupBy { sectionFor(it.asset, data) }

        return buildMap {
            for (section in SwapTokenSection.entries) {
                val sectionTokens = tokensBySection[section] ?: continue
                put(section, sectionTokens)
            }
        }
    }

    private fun sectionFor(asset: Asset, data: SwapTokenSelectionData): SwapTokenSection {
        val isIncompatible = data.hasOpposite && asset.token.configuration.fullId !in data.compatible

        return when {
            isIncompatible -> SwapTokenSection.INCOMPATIBLE
            asset.totalInPlanks.isPositive() -> SwapTokenSection.YOUR_TOKENS
            else -> SwapTokenSection.OTHER_TOKENS
        }
    }
}

private class SwapSelectionCatalogue(val chains: List<Chain>, val assetConfigurations: List<Chain.Asset>)

private class SwapSelectionAssets(val chains: List<Chain>, val assets: List<Asset>)

class SwapTokenSelectionData(
    val assets: List<Asset>,
    val chains: List<Chain>,
    val compatible: Set<FullChainAssetId>,
    val hasOpposite: Boolean,
    val oppositeSymbol: String? = null
)

enum class SwapTokenSection { YOUR_TOKENS, OTHER_TOKENS, INCOMPATIBLE }

private const val BIFROST_POLKADOT = "262e1b2ad728475fd6fe88e62d34c200abe6fd693931ddad144059b1eb884e5b"
