package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.DefaultTokensRepository
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.LoadMoreTokensPreferences
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.byNetworks
import io.novafoundation.nova.feature_assets.domain.assets.models.byTokens
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class AssetListMixinFactory(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val expandableAssetsMixinFactory: ExpandableAssetsMixinFactory,
    private val defaultTokensRepository: DefaultTokensRepository,
    private val loadMoreTokensPreferences: LoadMoreTokensPreferences
) {

    fun create(coroutineScope: CoroutineScope): AssetListMixin = RealAssetListMixin(
        walletInteractor,
        assetsListInteractor,
        externalBalancesInteractor,
        expandableAssetsMixinFactory,
        defaultTokensRepository,
        loadMoreTokensPreferences,
        coroutineScope
    )
}

interface AssetListMixin {

    val assetsViewModeFlow: Flow<AssetViewMode>

    val externalBalancesFlow: SharedFlow<List<ExternalBalance>>

    val assetsFlow: Flow<List<Asset>>

    val assetModelsFlow: Flow<List<BalanceListRvItem>>

    val defaultFilterActiveFlow: Flow<Boolean>

    fun expandToken(tokenGroupUi: TokenGroupUi)

    suspend fun switchViewMode()

    fun refreshUserAddedTokens()
}

class RealAssetListMixin(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val expandableAssetsMixinFactory: ExpandableAssetsMixinFactory,
    private val defaultTokensRepository: DefaultTokensRepository,
    private val loadMoreTokensPreferences: LoadMoreTokensPreferences,
    private val coroutineScope: CoroutineScope
) : AssetListMixin, CoroutineScope by coroutineScope {

    private val defaultAssetsFlow = MutableStateFlow<Set<FullChainAssetId>?>(null)

    private val hasUsedLoadMoreFlow = loadMoreTokensPreferences.hasUsedLoadMoreFlow()

    private val userAddedTokensFlow = loadMoreTokensPreferences.userAddedTokensFlow()

    init {
        coroutineScope.launch {
            val defaults = defaultTokensRepository.getDefaultAssets()
            defaultAssetsFlow.value = defaults
        }
    }

    override fun refreshUserAddedTokens() {
        // No-op: userAddedTokensFlow is now reactive via preferences
    }

    override val assetsFlow = walletInteractor.assetsFlow()
        .shareInBackground()

    private val filteredAssetsFlow = walletInteractor.filterAssets(assetsFlow)
        .shareInBackground()

    override val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()
        .shareInBackground()

    override val assetsViewModeFlow = assetsListInteractor.assetsViewModeFlow()
        .shareInBackground()

    private val defaultFilteredAssetsFlow = combine(
        filteredAssetsFlow,
        defaultAssetsFlow,
        hasUsedLoadMoreFlow,
        userAddedTokensFlow
    ) { assets, defaultAssets, hasUsedLoadMore, userAddedTokens ->
        applyDefaultFilter(assets, defaultAssets, hasUsedLoadMore, userAddedTokens)
    }.shareInBackground()

    override val defaultFilterActiveFlow: Flow<Boolean> = combine(
        filteredAssetsFlow,
        defaultAssetsFlow,
        hasUsedLoadMoreFlow
    ) { assets, defaultAssets, hasUsedLoadMore ->
        shouldApplyDefaultFilter(assets, defaultAssets, hasUsedLoadMore)
    }.shareInBackground()

    private val throttledBalance = combineToPair(defaultFilteredAssetsFlow, externalBalancesFlow)
        .throttleLast(300.milliseconds)

    private val assetsByViewMode = combine(
        throttledBalance,
        assetsViewModeFlow
    ) { (assets, externalBalances), viewMode ->
        when (viewMode) {
            AssetViewMode.NETWORKS -> walletInteractor.groupAssetsByNetwork(assets, externalBalances).byNetworks()
            AssetViewMode.TOKENS -> walletInteractor.groupAssetsByToken(assets, externalBalances).byTokens()
        }
    }.shareInBackground()

    private val expandableAssetsMixin = expandableAssetsMixinFactory.create(assetsByViewMode)

    override val assetModelsFlow = expandableAssetsMixin.assetModelsFlow
        .shareInBackground()

    override fun expandToken(tokenGroupUi: TokenGroupUi) {
        expandableAssetsMixin.expandToken(tokenGroupUi)
    }

    override suspend fun switchViewMode() {
        expandableAssetsMixin.switchViewMode()
    }

    private fun applyDefaultFilter(
        assets: List<Asset>,
        defaultAssets: Set<FullChainAssetId>?,
        hasUsedLoadMore: Boolean,
        userAddedTokens: Set<FullChainAssetId> = emptySet()
    ): List<Asset> {
        if (!shouldApplyDefaultFilter(assets, defaultAssets, hasUsedLoadMore)) {
            return assets
        }

        return assets.filter { asset ->
            val fullId = asset.token.configuration.fullId
            fullId in defaultAssets!! || fullId in userAddedTokens || asset.total.signum() > 0
        }
    }

    private fun shouldApplyDefaultFilter(
        assets: List<Asset>,
        defaultAssets: Set<FullChainAssetId>?,
        hasUsedLoadMore: Boolean
    ): Boolean {
        if (defaultAssets == null) return false
        if (hasUsedLoadMore) return false

        val isNewWallet = assets.all { it.total.signum() == 0 }
        return isNewWallet
    }
}
