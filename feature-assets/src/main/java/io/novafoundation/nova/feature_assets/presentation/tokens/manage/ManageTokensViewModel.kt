package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.checkEnabled
import io.novafoundation.nova.common.utils.combineIdentity
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFilter
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFiltersInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken
import io.novafoundation.nova.feature_assets.domain.tokens.manage.allChainAssetIds
import io.novafoundation.nova.feature_assets.domain.tokens.manage.isEnabled
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenMapper
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val commonUiMapper: MultiChainTokenMapper,
    private val assetFiltersInteractor: AssetFiltersInteractor,
) : BaseViewModel() {

    val filtersEnabledMap = assetFiltersInteractor.allFilters.associateWith { MutableStateFlow(false) }

    val query = MutableStateFlow("")

    private val multiChainTokensFlow = interactor.multiChainTokensFlow(query)
        .shareInBackground()

    val searchResults = multiChainTokensFlow
        .mapList(::mapMultiChainTokenToUi)
        .shareInBackground()

    init {
        applyFiltersInitialState()
    }

    fun closeClicked() {
        router.back()
    }

    fun addClicked() {
        router.openAddTokenSelectChain()
    }

    fun editClicked(position: Int) = launch {
        val token = getMultiChainTokenAt(position) ?: return@launch

        val payload = ManageChainTokensPayload(token.symbol)
        router.openManageChainTokens(payload)
    }

    fun enableTokenSwitchClicked(position: Int) = launch {
        val token = getMultiChainTokenAt(position) ?: return@launch

        interactor.updateEnabledState(
            enabled = !token.isEnabled(),
            assetIds = token.allChainAssetIds()
        )
    }

    private suspend fun getMultiChainTokenAt(position: Int): MultiChainToken? {
        return multiChainTokensFlow.first().getOrNull(position)
    }

    private fun mapMultiChainTokenToUi(multiChainToken: MultiChainToken): MultiChainTokenModel {
        return MultiChainTokenModel(
            header = commonUiMapper.mapHeaderToUi(multiChainToken),
            enabled = multiChainToken.isEnabled(),
            switchable = multiChainToken.isSwitchable
        )
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
}
