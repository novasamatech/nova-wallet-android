package io.novafoundation.nova.feature_assets.domain.tokens.manage

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.utils.isSubsetOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_assets.domain.tokens.AssetsDataCleaner
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AssetEnabledUpdate
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
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
import java.math.BigDecimal

interface ManageTokenInteractor {

    fun assetGroupsFlow(queryFlow: Flow<String>, viewModeFlow: Flow<AssetViewMode>): Flow<List<ManageAssetGroup>>

    suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>)
}

class RealManageTokenInteractor(
    private val chainRegistry: ChainRegistry,
    private val chainAssetRepository: ChainAssetRepository,
    private val assetsDataCleaner: AssetsDataCleaner,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val defaultAssetsRepository: DefaultAssetsRepository,
) : ManageTokenInteractor {

    private val changeTokensMutex = Mutex(false)

    override fun assetGroupsFlow(
        queryFlow: Flow<String>,
        viewModeFlow: Flow<AssetViewMode>
    ): Flow<List<ManageAssetGroup>> {
        return combine(chainRegistry.enabledChainsFlow(), queryFlow, viewModeFlow) { chains, query, viewMode ->
            val defaultAssetOrder = defaultAssetsRepository.defaultAssetIds()
                .withIndex()
                .associate { (index, assetId) -> assetId to index }

            val allAssets = chains.sortedWith(Chain.defaultComparator()).flatMap { chain ->
                chain.assets.map { asset -> ChainWithAsset(chain, asset) }
            }

            // Switchability is decided against every asset, not just the ones matching the query -
            // otherwise searching could make the last enabled token look safe to turn off.
            val enabledAssetIds = allAssets.filter { it.asset.enabled }.map { it.asset.fullId }

            val matching = allAssets.searchTokens(
                query = query,
                chainsById = chainRegistry.chainsById(),
                tokenSymbol = { it.asset.symbol.value },
                relevantToChains = { chainWithAsset, chainIds -> chainWithAsset.chain.id in chainIds }
            )

            val groups = when (viewMode) {
                AssetViewMode.TOKENS -> groupByToken(matching, enabledAssetIds, defaultAssetOrder)
                AssetViewMode.NETWORKS -> groupByNetwork(matching, enabledAssetIds, defaultAssetOrder)
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

    override suspend fun updateEnabledState(enabled: Boolean, assetIds: List<FullChainAssetId>) = withContext(Dispatchers.IO) {
        changeTokensMutex.withLock {
            if (!enabled && canNotDisableAssets(assetIds)) {
                return@withLock
            }

            // Read before the data is cleared below - afterwards every balance looks like zero,
            // and a token the user deliberately hid despite holding it would not be remembered.
            val withPositiveBalance = assetIdsWithPositiveBalance()

            val updates = assetIds.map { assetId ->
                AssetEnabledUpdate.byUser(assetId, enabled, hasPositiveBalance = assetId in withPositiveBalance)
            }

            chainAssetRepository.setAssetsEnabled(updates)

            if (!enabled) {
                assetsDataCleaner.clearAssetsData(assetIds)
            }
        }
    }

    private suspend fun assetIdsWithPositiveBalance(): Set<FullChainAssetId> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return walletRepository.getSyncedAssets(metaAccount.id)
            .filter { it.total > BigDecimal.ZERO }
            .mapTo(mutableSetOf()) { it.token.configuration.fullId }
    }

    private suspend fun canNotDisableAssets(assetIds: List<FullChainAssetId>): Boolean {
        val enabledAssets = chainAssetRepository.getEnabledAssets()
            .map { it.fullId }
        return assetIds.containsAll(enabledAssets)
    }

    private fun groupByToken(
        assets: List<ChainWithAsset>,
        enabledAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>
    ): List<ManageAssetGroup> {
        return assets.groupBy { (_, asset) -> asset.normalizeSymbol() }
            .map { (symbol, chainsWithAssets) ->
                buildGroup(
                    id = symbol,
                    title = symbol,
                    iconUrl = chainsWithAssets.first().asset.icon,
                    kind = ManageAssetGroup.Kind.TOKEN,
                    members = chainsWithAssets,
                    enabledAssetIds = enabledAssetIds,
                    defaultAssetOrder = defaultAssetOrder,
                    itemTitle = { it.chain.name },
                    itemSubtitle = { null },
                    itemIconUrl = { it.chain.icon }
                )
            }
    }

    private fun groupByNetwork(
        assets: List<ChainWithAsset>,
        enabledAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>
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
                    enabledAssetIds = enabledAssetIds,
                    defaultAssetOrder = defaultAssetOrder,
                    itemTitle = { it.asset.symbol.value },
                    itemSubtitle = { it.asset.name },
                    itemIconUrl = { it.asset.icon }
                )
            }
    }

    private fun buildGroup(
        id: String,
        title: String,
        iconUrl: String?,
        kind: ManageAssetGroup.Kind,
        members: List<ChainWithAsset>,
        enabledAssetIds: List<FullChainAssetId>,
        defaultAssetOrder: Map<FullChainAssetId, Int>,
        itemTitle: (ChainWithAsset) -> String,
        itemSubtitle: (ChainWithAsset) -> String?,
        itemIconUrl: (ChainWithAsset) -> String?
    ): ManageAssetGroup {
        val enabledInGroup = members.filter { it.asset.enabled }.map { it.asset.fullId }

        // The wallet must keep at least one visible asset, so the group that holds every remaining
        // enabled asset cannot be turned off, and neither can its last enabled member.
        val holdsAllEnabledAssets = enabledAssetIds.isSubsetOf(enabledInGroup)
        val holdsTheLastEnabledAsset = holdsAllEnabledAssets && enabledInGroup.size == 1

        return ManageAssetGroup(
            id = id,
            title = title,
            iconUrl = iconUrl,
            kind = kind,
            isDefault = members.any { it.asset.fullId in defaultAssetOrder },
            isEnabled = enabledInGroup.isNotEmpty(),
            isSwitchable = !holdsAllEnabledAssets,
            items = members.map { member ->
                ManageAssetItem(
                    id = member.asset.fullId,
                    title = itemTitle(member),
                    subtitle = itemSubtitle(member),
                    iconUrl = itemIconUrl(member),
                    isEnabled = member.asset.enabled,
                    isSwitchable = !member.asset.enabled || !holdsTheLastEnabledAsset
                )
            }
        )
    }
}
