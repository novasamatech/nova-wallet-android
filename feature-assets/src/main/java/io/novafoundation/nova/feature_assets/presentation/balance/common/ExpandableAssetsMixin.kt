package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.switch
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.combineToQuad
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.NetworkAssetMapper
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.NetworkAssetMapperFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.TokenAssetMapper
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.TokenAssetMapperFactory
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

class ExpandableAssetsMixinFactory(
    private val assetIconProvider: AssetIconProvider,
    private val currencyInteractor: CurrencyInteractor,
    private val assetsViewModeRepository: AssetsViewModeRepository,
    private val amountFormatterProvider: MaskableValueFormatterProvider,
    private val networkAssetMapperFactory: NetworkAssetMapperFactory,
    private val tokenAssetMapperFactory: TokenAssetMapperFactory,
) {

    fun create(assetsFlow: Flow<AssetsByViewModeResult>): ExpandableAssetsMixin {
        return RealExpandableAssetsMixin(
            assetsFlow,
            currencyInteractor,
            amountFormatterProvider,
            networkAssetMapperFactory,
            tokenAssetMapperFactory,
            assetIconProvider,
            assetsViewModeRepository
        )
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
    amountFormatterProvider: MaskableValueFormatterProvider,
    networkAssetMapperFactory: NetworkAssetMapperFactory,
    tokenAssetMapperFactory: TokenAssetMapperFactory,
    private val assetIconProvider: AssetIconProvider,
    private val assetsViewModeRepository: AssetsViewModeRepository
) : ExpandableAssetsMixin {

    private val assetsFormatters = amountFormatterProvider.provideFormatter()
        .map {
            AssetMappers(
                networkAssetMapperFactory.create(it),
                tokenAssetMapperFactory.create(it)
            )
        }

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()

    private val expandedTokenIdsFlow = MutableStateFlow(setOf<String>())

    override val assetModelsFlow: Flow<List<BalanceListRvItem>> = combineToQuad(
        assetsFlow,
        expandedTokenIdsFlow,
        selectedCurrency,
        assetsFormatters
    ).mapLatest { (assetsByViewMode, expandedTokens, currency, assetMappers) ->
        when (assetsByViewMode) {
            is AssetsByViewModeResult.ByNetworks -> assetMappers.networkAssetMapper.mapGroupedAssetsToUi(
                groupedAssets = assetsByViewMode.assets,
                assetIconProvider = assetIconProvider,
                currency = currency
            )

            is AssetsByViewModeResult.ByTokens -> assetMappers.tokenAssetMapper.mapGroupedAssetsToUi(
                groupedTokens = assetsByViewMode.tokens,
                assetIconProvider = assetIconProvider,
                assetFilter = { groupId, assetsInGroup -> filterTokens(groupId, assetsInGroup, expandedTokens) }
            )
        }
    }
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

private class AssetMappers(
    val networkAssetMapper: NetworkAssetMapper,
    val tokenAssetMapper: TokenAssetMapper
)
