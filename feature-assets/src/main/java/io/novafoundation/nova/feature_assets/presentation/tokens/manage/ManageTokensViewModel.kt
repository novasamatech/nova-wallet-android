package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.checkEnabled
import io.novafoundation.nova.common.utils.combineIdentity
import io.novafoundation.nova.common.utils.images.asUrlIcon
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.DustFilterPreferences
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.DefaultTokensRepository
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.LoadMoreTokensPreferences
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFilter
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFiltersInteractor
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken
import io.novafoundation.nova.feature_assets.domain.tokens.manage.NetworkGroup
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageTokensRvItem
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageTokensTab
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val assetFiltersInteractor: AssetFiltersInteractor,
    private val assetIconProvider: AssetIconProvider,
    private val loadMoreTokensPreferences: LoadMoreTokensPreferences,
    private val dustFilterPreferences: DustFilterPreferences,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val defaultTokensRepository: DefaultTokensRepository,
) : BaseViewModel() {

    companion object {
        private val BRIDGE_SUFFIX_REGEX = Regex("-(SNOWBRIDGE|WORMHOLE).*", RegexOption.IGNORE_CASE)

        private val TOKEN_PRIORITY_ORDER = listOf("DOT", "KSM", "USDC", "USDT", "ETH", "HOLLAR", "TBTC", "HDX", "SOL")

        private val NETWORK_PRIORITY_CHAIN_IDS = mapOf(
            "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f" to 0, // Polkadot Asset Hub
            "48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a" to 1, // Kusama Asset Hub
            "eip155:1" to 2, // Ethereum
            "afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d" to 3  // Hydration
        )

        private val SYSTEM_CHAIN_KEYWORDS = listOf("People", "Coretime", "Bridge Hub", "Collectives")

        private fun normalizeForPriority(symbol: String): String {
            return symbol.uppercase()
                .replace(BRIDGE_SUFFIX_REGEX, "")
        }

        private fun tokenPriorityIndex(symbol: String): Int {
            val normalized = normalizeForPriority(symbol)
            val index = TOKEN_PRIORITY_ORDER.indexOfFirst { it.equals(normalized, ignoreCase = true) }
            return if (index >= 0) index else Int.MAX_VALUE
        }

        private fun networkPriorityIndex(chain: Chain): Int {
            val chainIdPriority = NETWORK_PRIORITY_CHAIN_IDS[chain.id]
            if (chainIdPriority != null) return chainIdPriority

            val isSystemChain = SYSTEM_CHAIN_KEYWORDS.any { keyword ->
                chain.name.contains(keyword, ignoreCase = true)
            }
            return if (isSystemChain) 4 else 5
        }

        private val manageTokenComparator = Comparator<MultiChainToken> { a, b ->
            val priorityA = tokenPriorityIndex(a.symbol)
            val priorityB = tokenPriorityIndex(b.symbol)
            if (priorityA != priorityB) {
                priorityA.compareTo(priorityB)
            } else {
                a.symbol.compareTo(b.symbol, ignoreCase = true)
            }
        }

        private val manageNetworkComparator = Comparator<NetworkGroup> { a, b ->
            val priorityA = networkPriorityIndex(a.chain)
            val priorityB = networkPriorityIndex(b.chain)
            if (priorityA != priorityB) {
                priorityA.compareTo(priorityB)
            } else {
                a.chain.name.compareTo(b.chain.name, ignoreCase = true)
            }
        }
    }

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    private val defaultAssetsFlow = MutableStateFlow<Set<FullChainAssetId>?>(null)
    private val userAddedTokensFlow = loadMoreTokensPreferences.userAddedTokensFlow()
        .shareInBackground()
    private val isDefaultFilterActiveFlow = MutableStateFlow(false)

    val filtersEnabledMap = assetFiltersInteractor.allFilters.associateWith { MutableStateFlow(false) }

    val dustFilterEnabled = MutableStateFlow(false)
    val dustFilterThreshold = MutableStateFlow(BigDecimal.ONE)

    val zeroBalanceFilterEnabled: MutableStateFlow<Boolean> =
        filtersEnabledMap[NonZeroBalanceFilter] ?: MutableStateFlow(false)

    val query = MutableStateFlow("")

    val currentTab = MutableStateFlow(ManageTokensTab.TOKENS)

    private val expandedSectionId = MutableStateFlow<String?>(null)

    private val multiChainTokensFlow = interactor.mergedMultiChainTokensFlow(query)
        .shareInBackground()

    private val networkGroupsFlow = interactor.networkGroupsFlow(query)
        .shareInBackground()

    private val visibleIdsOverrideFlow = combine(
        isDefaultFilterActiveFlow,
        defaultAssetsFlow,
        userAddedTokensFlow
    ) { isActive, defaults, userAdded ->
        if (isActive && defaults != null) defaults + userAdded else null
    }.shareInBackground()

    val listItems = combine(
        currentTab,
        multiChainTokensFlow,
        networkGroupsFlow,
        expandedSectionId,
        visibleIdsOverrideFlow
    ) { tab, tokens, networks, expandedId, visibleIds ->
        when (tab) {
            ManageTokensTab.TOKENS -> buildTokenTabItems(tokens, expandedId, visibleIds)
            ManageTokensTab.NETWORKS -> buildNetworkTabItems(networks, expandedId, visibleIds)
        }
    }.shareInBackground()

    val allSelected = combine(
        currentTab,
        multiChainTokensFlow,
        networkGroupsFlow,
        visibleIdsOverrideFlow
    ) { tab, tokens, networks, visibleIds ->
        if (visibleIds != null) {
            when (tab) {
                ManageTokensTab.TOKENS -> tokens.isNotEmpty() && tokens.all { token ->
                    token.instances.all { FullChainAssetId(it.chain.id, it.chainAssetId) in visibleIds }
                }
                ManageTokensTab.NETWORKS -> networks.isNotEmpty() && networks.all { group ->
                    group.tokens.all { FullChainAssetId(it.chain.id, it.asset.id) in visibleIds }
                }
            }
        } else {
            when (tab) {
                ManageTokensTab.TOKENS -> tokens.isNotEmpty() && tokens.all { token -> token.instances.all { it.isEnabled } }
                ManageTokensTab.NETWORKS -> networks.isNotEmpty() && networks.all { group -> group.tokens.all { it.isEnabled } }
            }
        }
    }.shareInBackground()

    val selectAllButtonTextRes = combine(allSelected, query.map { it.isNotEmpty() }) { allSel, hasQuery ->
        when {
            allSel && hasQuery -> io.novafoundation.nova.common.R.string.manage_tokens_deselect_visible
            allSel -> io.novafoundation.nova.common.R.string.manage_tokens_deselect_all
            hasQuery -> io.novafoundation.nova.common.R.string.manage_tokens_select_visible
            else -> io.novafoundation.nova.common.R.string.manage_tokens_select_all
        }
    }.shareInBackground()

    init {
        applyFiltersInitialState()
        initDustFilterState()
        query.drop(1).onEach { expandedSectionId.value = null }.launchIn(viewModelScope)

        launch {
            val defaults = defaultTokensRepository.getDefaultAssets()
            defaultAssetsFlow.value = defaults

            // Determine if the default filter is active by checking the same logic as AssetListMixin:
            // no hasUsedLoadMore, and defaults are available
            // The actual "new wallet" check happens via the all-zero-balance condition in AssetListMixin
            // For the manage screen, we use the same hasUsedLoadMore flag as the primary gate
            val hasUsedLoadMore = loadMoreTokensPreferences.hasUsedLoadMore()
            isDefaultFilterActiveFlow.value = defaults != null && !hasUsedLoadMore
        }
    }

    fun closeClicked() {
        router.back()
    }

    fun addClicked() {
        router.openAddTokenSelectChain()
    }

    fun tabSelected(tab: ManageTokensTab) {
        currentTab.value = tab
        expandedSectionId.value = null
    }

    fun headerClicked(headerId: String) {
        expandedSectionId.value = if (expandedSectionId.value == headerId) null else headerId
    }

    fun childToggled(item: ManageTokensRvItem.Child) = launch {
        if (isDefaultFilterActiveFlow.value && defaultAssetsFlow.value != null) {
            // When default filter is active, toggle user-added set instead of DB
            val newEnabled = !item.isEnabled
            val defaults = defaultAssetsFlow.value ?: emptySet()
            val currentUserAdded = userAddedTokensFlow.first()
            if (newEnabled) {
                if (item.chainAssetId !in defaults) {
                    loadMoreTokensPreferences.addUserAddedToken(item.chainAssetId)
                }
            } else {
                if (item.chainAssetId in currentUserAdded) {
                    loadMoreTokensPreferences.removeUserAddedToken(item.chainAssetId)
                }
            }
        } else {
            loadMoreTokensPreferences.setHasUsedLoadMore(true)

            interactor.updateEnabledState(
                enabled = !item.isEnabled,
                assetIds = listOf(item.chainAssetId)
            )
        }
    }

    fun selectAllClicked() = launch {
        val isAllSelected = allSelected.first()
        val enabled = !isAllSelected

        val visibleAssetIds = when (currentTab.value) {
            ManageTokensTab.TOKENS -> multiChainTokensFlow.first().flatMap { token ->
                token.instances.map { FullChainAssetId(it.chain.id, it.chainAssetId) }
            }
            ManageTokensTab.NETWORKS -> networkGroupsFlow.first().flatMap { group ->
                group.tokens.map { FullChainAssetId(it.chain.id, it.asset.id) }
            }
        }

        if (isDefaultFilterActiveFlow.value && defaultAssetsFlow.value != null) {
            val defaults = defaultAssetsFlow.value ?: emptySet()
            if (enabled) {
                // Add all non-default visible tokens to user-added set
                for (id in visibleAssetIds) {
                    if (id !in defaults) {
                        loadMoreTokensPreferences.addUserAddedToken(id)
                    }
                }
            } else {
                // Remove all user-added tokens (defaults stay)
                val currentUserAdded = userAddedTokensFlow.first()
                for (id in visibleAssetIds) {
                    if (id in currentUserAdded) {
                        loadMoreTokensPreferences.removeUserAddedToken(id)
                    }
                }
            }
        } else {
            loadMoreTokensPreferences.setHasUsedLoadMore(true)

            if (enabled) {
                interactor.updateEnabledState(enabled = true, assetIds = visibleAssetIds)
            } else {
                confirmationAwaitableAction.awaitAction(
                    ConfirmationDialogInfo(
                        resourceManager.getString(io.novafoundation.nova.common.R.string.manage_tokens_deselect_all_confirmation_title),
                        resourceManager.getString(io.novafoundation.nova.common.R.string.manage_tokens_deselect_all_confirmation_message),
                        resourceManager.getString(io.novafoundation.nova.common.R.string.common_confirm),
                        resourceManager.getString(io.novafoundation.nova.common.R.string.common_cancel)
                    )
                )

                // Keep DOT on Polkadot Asset Hub enabled to satisfy the safety guard
                val keepEnabled = FullChainAssetId(ChainGeneses.POLKADOT_ASSET_HUB, 0)
                val toDisable = visibleAssetIds.filter { it != keepEnabled }
                if (toDisable.isNotEmpty()) {
                    interactor.updateEnabledState(enabled = false, assetIds = toDisable)
                }

                showToast(resourceManager.getString(io.novafoundation.nova.common.R.string.manage_tokens_at_least_one))
            }
        }
    }

    private fun buildTokenTabItems(
        tokens: List<MultiChainToken>,
        expandedId: String?,
        visibleIdsOverride: Set<FullChainAssetId>? = null
    ): List<ManageTokensRvItem> {
        val sortedTokens = tokens.sortedWith(manageTokenComparator)
        return buildList {
            for (token in sortedTokens) {
                val isExpanded = token.id == expandedId

                val enabledCount = if (visibleIdsOverride != null) {
                    token.instances.count { FullChainAssetId(it.chain.id, it.chainAssetId) in visibleIdsOverride }
                } else {
                    token.instances.count { it.isEnabled }
                }

                val hasMixedSymbols = token.instances.map { it.originalSymbol }.distinct().size > 1

                add(
                    ManageTokensRvItem.Header(
                        id = token.id,
                        title = token.symbol,
                        icon = assetIconProvider.getAssetIconOrFallback(token.icon),
                        enabledCount = enabledCount,
                        totalCount = token.instances.size,
                        isExpanded = isExpanded,
                    )
                )

                if (isExpanded) {
                    for (instance in token.instances) {
                        val fullId = FullChainAssetId(instance.chain.id, instance.chainAssetId)
                        val isEnabled = visibleIdsOverride?.contains(fullId) ?: instance.isEnabled

                        val childName = if (hasMixedSymbols) {
                            "${instance.originalSymbol} on ${instance.chain.name}"
                        } else {
                            instance.chain.name
                        }
                        add(
                            ManageTokensRvItem.Child(
                                chainAssetId = fullId,
                                name = childName,
                                icon = instance.chain.icon?.asUrlIcon(),
                                isEnabled = isEnabled,
                                isSwitchable = instance.isSwitchable,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun buildNetworkTabItems(
        networks: List<NetworkGroup>,
        expandedId: String?,
        visibleIdsOverride: Set<FullChainAssetId>? = null
    ): List<ManageTokensRvItem> {
        val sortedNetworks = networks.sortedWith(manageNetworkComparator)
        return buildList {
            for (group in sortedNetworks) {
                val isExpanded = group.chain.id == expandedId

                val enabledCount = if (visibleIdsOverride != null) {
                    group.tokens.count { FullChainAssetId(it.chain.id, it.asset.id) in visibleIdsOverride }
                } else {
                    group.tokens.count { it.isEnabled }
                }

                add(
                    ManageTokensRvItem.Header(
                        id = group.chain.id,
                        title = group.chain.name,
                        icon = group.chain.icon?.asUrlIcon(),
                        enabledCount = enabledCount,
                        totalCount = group.tokens.size,
                        isExpanded = isExpanded,
                    )
                )

                if (isExpanded) {
                    for (token in group.tokens) {
                        val fullId = FullChainAssetId(token.chain.id, token.asset.id)
                        val isEnabled = visibleIdsOverride?.contains(fullId) ?: token.isEnabled

                        add(
                            ManageTokensRvItem.Child(
                                chainAssetId = fullId,
                                name = token.asset.normalizeSymbol(),
                                icon = assetIconProvider.getAssetIconOrFallback(token.asset.icon),
                                isEnabled = isEnabled,
                                isSwitchable = token.isSwitchable,
                            )
                        )
                    }
                }
            }
        }
    }

    private fun applyFiltersInitialState() = launch {
        val initialFilters = assetFiltersInteractor.currentFilters()

        filtersEnabledMap.forEach { (filter, checked) ->
            checked.value = filter in initialFilters
        }

        filtersEnabledMap.applyOnChange()
    }

    private fun Map<AssetFilter, MutableStateFlow<Boolean>>.applyOnChange() {
        combineIdentity(this.values)
            .drop(1)
            .onEach {
                val enabledFilters = assetFiltersInteractor.allFilters.filter(filtersEnabledMap::checkEnabled)
                assetFiltersInteractor.updateFilters(enabledFilters)
            }
            .launchIn(viewModelScope)
    }

    private fun initDustFilterState() {
        dustFilterEnabled.value = dustFilterPreferences.isDustFilterEnabled()
        dustFilterThreshold.value = dustFilterPreferences.getThreshold()

        dustFilterEnabled.drop(1).onEach { enabled ->
            dustFilterPreferences.setDustFilterEnabled(enabled)
        }.launchIn(viewModelScope)

        dustFilterThreshold.drop(1).onEach { threshold ->
            dustFilterPreferences.setThreshold(threshold)
        }.launchIn(viewModelScope)
    }

    fun dustFilterToggled(enabled: Boolean) {
        dustFilterEnabled.value = enabled
    }

    fun dustThresholdSelected(threshold: BigDecimal) {
        dustFilterThreshold.value = threshold
    }
}
