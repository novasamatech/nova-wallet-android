package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.byNetworks
import io.novafoundation.nova.feature_assets.domain.assets.models.byTokens
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration.Companion.milliseconds

class AssetListMixinFactory(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val expandableAssetsMixinFactory: ExpandableAssetsMixinFactory
) {

    fun create(coroutineScope: CoroutineScope): AssetListMixin = RealAssetListMixin(
        walletInteractor,
        assetsListInteractor,
        externalBalancesInteractor,
        expandableAssetsMixinFactory,
        coroutineScope
    )
}

interface AssetListMixin {

    val assetsViewModeFlow: Flow<AssetViewMode>

    val externalBalancesFlow: SharedFlow<List<ExternalBalance>>

    val assetsFlow: Flow<List<Asset>>

    val assetModelsFlow: Flow<List<BalanceListRvItem>>

    fun expandToken(tokenGroupUi: TokenGroupUi)

    suspend fun switchViewMode()
}

class RealAssetListMixin(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val expandableAssetsMixinFactory: ExpandableAssetsMixinFactory,
    private val coroutineScope: CoroutineScope
) : AssetListMixin, CoroutineScope by coroutineScope {

    override val assetsFlow = walletInteractor.assetsFlow()
        .shareInBackground()

    private val filteredAssetsFlow = walletInteractor.filterAssets(assetsFlow)
        .shareInBackground()

    override val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()
        .shareInBackground()

    override val assetsViewModeFlow = assetsListInteractor.assetsViewModeFlow()
        .shareInBackground()

    private val throttledBalance = combineToPair(filteredAssetsFlow, externalBalancesFlow)
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
}
