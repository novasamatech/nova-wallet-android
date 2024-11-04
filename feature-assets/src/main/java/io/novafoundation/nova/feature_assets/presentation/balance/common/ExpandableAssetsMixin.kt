package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.switch
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class ExpandableAssetsMixinFactory(
    private val resourceManager: ResourceManager,
    private val currencyInteractor: CurrencyInteractor,
    private val assetsViewModeRepository: AssetsViewModeRepository
) {

    fun create(assetsFlow: Flow<AssetsByViewModeResult>): ExpandableAssetsMixin {
        return RealExpandableAssetsMixin(resourceManager, assetsFlow, currencyInteractor, assetsViewModeRepository)
    }
}

interface ExpandableAssetsMixin {

    val assetModelsFlow: Flow<List<BalanceListRvItem>>

    fun expandToken(tokenGroupUi: TokenGroupUi)
    suspend fun switchViewMode()
}

class RealExpandableAssetsMixin(
    private val resourceManager: ResourceManager,
    assetsFlow: Flow<AssetsByViewModeResult>,
    currencyInteractor: CurrencyInteractor,
    private val assetsViewModeRepository: AssetsViewModeRepository,
) : ExpandableAssetsMixin {

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()

    private val expandedTokenIdsFlow = MutableStateFlow(setOf<String>())

    override val assetModelsFlow: Flow<List<BalanceListRvItem>> = combine(
        assetsFlow,
        expandedTokenIdsFlow,
        selectedCurrency
    ) { assetesByViewMode, expandedTokens, currency ->
        when (assetesByViewMode) {
            is AssetsByViewModeResult.ByNetworks -> assetesByViewMode.assets.mapGroupedAssetsToUi(resourceManager, currency)
            is AssetsByViewModeResult.ByTokens -> assetesByViewMode.tokens.mapGroupedAssetsToUi(
                resourceManager = resourceManager,
                assetFilter = { groupId, assetsInGroup -> filterTokens(groupId, assetsInGroup, expandedTokens) }
            )
        }
    }

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
