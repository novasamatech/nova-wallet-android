package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.switch
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.combineToTriple
import io.novafoundation.nova.common.utils.throttleLast
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.Duration.Companion.milliseconds

class ExpandableAssetsMixinFactory(
    private val assetIconProvider: AssetIconProvider,
    private val currencyInteractor: CurrencyInteractor,
    private val assetsViewModeRepository: AssetsViewModeRepository,
    private val amountFormatter: AmountFormatter
) {

    fun create(assetsFlow: Flow<AssetsByViewModeResult>): ExpandableAssetsMixin {
        return RealExpandableAssetsMixin(assetsFlow, currencyInteractor, assetIconProvider, assetsViewModeRepository, amountFormatter)
    }
}

interface ExpandableAssetsMixin {

    val assetModelsFlow: Flow<List<BalanceListRvItem>>

    fun expandToken(tokenGroupUi: TokenGroupUi)

    suspend fun switchViewMode()
}

class RealExpandableAssetsMixin(
    assetsFlow: Flow<AssetsByViewModeResult>,
    currencyInteractor: CurrencyInteractor,
    private val assetIconProvider: AssetIconProvider,
    private val assetsViewModeRepository: AssetsViewModeRepository,
    private val amountFormatter: AmountFormatter
) : ExpandableAssetsMixin {

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()

    private val expandedTokenIdsFlow = MutableStateFlow(setOf<String>())

    override val assetModelsFlow: Flow<List<BalanceListRvItem>> = combineToTriple(
        assetsFlow,
        expandedTokenIdsFlow,
        selectedCurrency
    )
        .mapLatest { (assetsByViewMode, expandedTokens, currency) ->
            when (assetsByViewMode) {
                is AssetsByViewModeResult.ByNetworks -> assetsByViewMode.assets.mapGroupedAssetsToUi(amountFormatter, assetIconProvider, currency)
                is AssetsByViewModeResult.ByTokens -> assetsByViewMode.tokens.mapGroupedAssetsToUi(
                    amountFormatter = amountFormatter,
                    assetIconProvider = assetIconProvider,
                    assetFilter = { groupId, assetsInGroup -> filterTokens(groupId, assetsInGroup, expandedTokens) }
                )
            }
        }.throttleLast(300.milliseconds)
        .distinctUntilChanged()

    override fun expandToken(tokenGroupUi: TokenGroupUi) {
        expandedTokenIdsFlow.updateValue { it.toggle(tokenGroupUi.itemId) }
    }

    override suspend fun switchViewMode() {
        expandedTokenIdsFlow.value = emptySet()

        val assetViewMode = assetsViewModeRepository.getAssetViewMode()
        assetsViewModeRepository.setAssetsViewMode(assetViewMode.switch())
    }

    private fun filterTokens(groupId: String, assets: List<TokenAssetUi>, expandedGroups: Set<String>): List<TokenAssetUi> {
        if (groupId in expandedGroups) {
            return filterIfSingleItem(assets)
        }

        return emptyList()
    }

    private fun filterIfSingleItem(assets: List<TokenAssetUi>): List<TokenAssetUi> {
        return if (assets.size <= 1) {
            emptyList()
        } else {
            assets
        }
    }
}
