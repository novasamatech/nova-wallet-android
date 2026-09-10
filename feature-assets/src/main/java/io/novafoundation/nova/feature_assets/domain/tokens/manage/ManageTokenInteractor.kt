package io.novafoundation.nova.feature_assets.domain.tokens.manage

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.utils.isSubsetOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetVisibilityRepository
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.DefaultAssetsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novafoundation.nova.runtime.multiNetwork.enabledChainsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ManageTokenInteractor {

    fun assetGroupsFlow(queryFlow: Flow<String>, viewModeFlow: Flow<AssetViewMode>): Flow<List<ManageAssetGroup>>

    suspend fun updateVisibility(visible: Boolean, assetIds: List<FullChainAssetId>)
}

class RealManageTokenInteractor(
    private val chainRegistry: ChainRegistry,
    private val assetVisibilityRepository: AssetVisibilityRepository,
    private val assetVisibilityUseCase: AssetVisibilityUseCase,
    private val accountRepository: AccountRepository,
    private val defaultAssetsRepository: DefaultAssetsRepository,
) : ManageTokenInteractor {

    private val changeTokensMutex = Mutex(false)

    override fun assetGroupsFlow(
        queryFlow: Flow<String>,
        viewModeFlow: Flow<AssetViewMode>
    ): Flow<List<ManageAssetGroup>> {
        // Visibility comes in as a flow rather than a snapshot: flipping a switch on this very screen
        // is what changes it, and the list has to answer for that without being reloaded.
        return combine(
            chainRegistry.enabledChainsFlow(),
            queryFlow,
            viewModeFlow,
            assetVisibilityUseCase.visibilityFlow()
        ) { chains, query, viewMode, visibility ->
            val defaultAssetOrder = defaultAssetsRepository.defaultAssetIds()
                .withIndex()
                .associate { (index, assetId) -> assetId to index }

            val allAssets = chains.sortedWith(Chain.defaultComparator()).flatMap { chain ->
                chain.assets.map { asset -> ChainWithAsset(chain, asset) }
            }

            // Switchability is decided against every asset, not just the ones matching the query -
            // otherwise searching could make the last visible token look safe to turn off.
            val isVisible = { chainWithAsset: ChainWithAsset -> visibility.isVisible(chainWithAsset.asset.fullId) }

            val visibleAssetIds = allAssets.filter(isVisible).map { it.asset.fullId }

            val matching = allAssets.searchTokens(
                query = query,
                chainsById = chainRegistry.chainsById(),
                tokenSymbol = { it.asset.symbol.value },
                relevantToChains = { chainWithAsset, chainIds -> chainWithAsset.chain.id in chainIds }
            )

            val groups = when (viewMode) {
                AssetViewMode.TOKENS -> groupByToken(matching, allAssets.tokenGroupKeys(), visibleAssetIds, defaultAssetOrder, isVisible)
                AssetViewMode.NETWORKS -> groupByNetwork(matching, visibleAssetIds, defaultAssetOrder, isVisible)
            }

            groups.sortedByDefaultsFirst(defaultAssetOrder)
        }
    }

    /**
     * Default groups lead, in the order the config lists them; everything else keeps the order the
     * grouping produced, since sorting here is stable.
     */
    private fun List<ManageAssetGroup>.sortedByDefaultsFirst(
        defaultAssetOrder: Map<FullChainAssetId, Int>
    ): List<ManageAssetGroup> {
        return sortedBy { group -> group.defaultOrder(defaultAssetOrder) ?: Int.MAX_VALUE }
    }

    private fun ManageAssetGroup.defaultOrder(defaultAssetOrder: Map<FullChainAssetId, Int>): Int? {
        return items.mapNotNull { defaultAssetOrder[it.id] }.minOrNull()
    }

    /**
     * Always writes the decision down, both ways. A hide has to be recorded even when the token was
     * not on screen anyway, otherwise balance discovery would show it again on the next sync.
     */
    override suspend fun updateVisibility(visible: Boolean, assetIds: List<FullChainAssetId>) = withContext(Dispatchers.IO) {
        changeTokensMutex.withLock {
            val metaId = accountRepository.getSelectedMetaAccount().id

            assetVisibilityRepository.setVisibility(metaId, assetIds.associateWith { visible })
        }
    }

    /**
     * Bridged variants name the bridge in their symbol - ETH-Snowbridge, SOL-Wormhole - and the
     * design puts them under the row of the token they stand for rather than on rows of their own.
     *
     * The suffix is only dropped when the plain symbol is itself a token in the list, so a token
     * that merely happens to contain a dash keeps its own group. Keys are computed over every asset
     * rather than the ones matching the search, otherwise searching for the variant alone would
     * hide the parent and split the group back apart.
     */
    private fun List<ChainWithAsset>.tokenGroupKeys(): Map<FullChainAssetId, String> {
        val symbols = mapTo(mutableSetOf()) { it.asset.normalizeSymbol() }

        return associate { chainWithAsset ->
            chainWithAsset.asset.fullId to tokenGroupKey(chainWithAsset.asset.normalizeSymbol(), symbols)
        }
    }

    private fun groupByToken(
        assets: List<ChainWithAsset>,
        groupKeys: Map<FullChainAssetId, String>,
        visibleAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>,
        isVisible: (ChainWithAsset) -> Boolean
    ): List<ManageAssetGroup> {
        return assets.groupBy { groupKeys.getValue(it.asset.fullId) }
            .map { (symbol, chainsWithAssets) ->
                // The group speaks for the parent token, so it takes the parent's icon where one is
                // present instead of whichever variant happened to sort first.
                val face = chainsWithAssets.firstOrNull { it.asset.normalizeSymbol() == symbol } ?: chainsWithAssets.first()

                buildGroup(
                    id = symbol,
                    title = symbol,
                    iconUrl = face.asset.icon,
                    kind = ManageAssetGroup.Kind.TOKEN,
                    members = chainsWithAssets,
                    visibleAssetIds = visibleAssetIds,
                    defaultAssetOrder = defaultAssetOrder,
                    itemTitle = { it.chain.name },
                    // Named only for a variant, where the network alone would not say which of the
                    // group's tokens the row switches - and two variants can share a network.
                    itemSubtitle = { it.asset.symbol.value.takeIf { _ -> it.asset.normalizeSymbol() != symbol } },
                    itemIconUrl = { it.chain.icon },
                    isVisible = isVisible
                )
            }
    }

    private fun groupByNetwork(
        assets: List<ChainWithAsset>,
        visibleAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>,
        isVisible: (ChainWithAsset) -> Boolean
    ): List<ManageAssetGroup> {
        return assets.groupBy { (chain, _) -> chain.id }
            .map { (_, chainsWithAssets) ->
                val chain = chainsWithAssets.first().chain

                buildGroup(
                    id = chain.id,
                    title = chain.name,
                    iconUrl = chain.icon,
                    kind = ManageAssetGroup.Kind.NETWORK,
                    members = chainsWithAssets,
                    visibleAssetIds = visibleAssetIds,
                    defaultAssetOrder = defaultAssetOrder,
                    itemTitle = { it.asset.symbol.value },
                    itemSubtitle = { it.asset.name },
                    itemIconUrl = { it.asset.icon },
                    isVisible = isVisible
                )
            }
    }

    private fun buildGroup(
        id: String,
        title: String,
        iconUrl: String?,
        kind: ManageAssetGroup.Kind,
        members: List<ChainWithAsset>,
        visibleAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>,
        itemTitle: (ChainWithAsset) -> String,
        itemSubtitle: (ChainWithAsset) -> String?,
        itemIconUrl: (ChainWithAsset) -> String?,
        isVisible: (ChainWithAsset) -> Boolean
    ): ManageAssetGroup {
        val visibleInGroup = members.filter(isVisible).map { it.asset.fullId }

        // The wallet must keep at least one visible asset, so the group that holds every remaining
        // visible asset cannot be turned off, and neither can its last visible member.
        val holdsAllVisibleAssets = visibleAssetIds.isSubsetOf(visibleInGroup)
        val holdsTheLastVisibleAsset = holdsAllVisibleAssets && visibleInGroup.size == 1

        return ManageAssetGroup(
            id = id,
            title = title,
            iconUrl = iconUrl,
            kind = kind,
            isDefault = members.any { it.asset.fullId in defaultAssetOrder },
            isEnabled = visibleInGroup.isNotEmpty(),
            isSwitchable = !holdsAllVisibleAssets,
            items = members.map { member ->
                ManageAssetItem(
                    id = member.asset.fullId,
                    title = itemTitle(member),
                    subtitle = itemSubtitle(member),
                    iconUrl = itemIconUrl(member),
                    isEnabled = isVisible(member),
                    isSwitchable = !isVisible(member) || !holdsTheLastVisibleAsset
                )
            }
        )
    }
}

private const val BRIDGED_SYMBOL_SEPARATOR = '-'

/**
 * The row a token belongs under: its own symbol, or the parent it is a bridged variant of.
 *
 * @param allSymbols every symbol the list has, so a variant only folds into a parent that is
 * actually there to fold into
 */
fun tokenGroupKey(symbol: String, allSymbols: Set<String>): String {
    val parent = symbol.substringBefore(BRIDGED_SYMBOL_SEPARATOR)

    return if (parent != symbol && parent in allSymbols) parent else symbol
}
