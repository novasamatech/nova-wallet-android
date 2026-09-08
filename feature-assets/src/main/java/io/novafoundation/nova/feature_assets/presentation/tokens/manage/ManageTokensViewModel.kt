package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.common.data.repository.AutoEnableTokensRepository
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageAssetGroup
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.allAssetIds
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsMapper
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsRvItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val mapper: ManageAssetsMapper,
    private val autoEnableTokensRepository: AutoEnableTokensRepository,
    assetsViewModeRepository: AssetsViewModeRepository,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    val autoEnableTokens = MutableStateFlow(true)

    private val expandedGroupIds = MutableStateFlow(emptySet<String>())

    private val groupsFlow = interactor.assetGroupsFlow(query, assetsViewModeRepository.assetsViewModeFlow())
        .shareInBackground()

    val listItems = combine(groupsFlow, expandedGroupIds, query) { groups, expanded, currentQuery ->
        mapper.mapGroupsToUi(groups, expanded, searching = currentQuery.isNotEmpty())
    }.shareInBackground()

    init {
        launch { autoEnableTokens.value = autoEnableTokensRepository.autoEnableTokens() }
    }

    fun closeClicked() {
        router.back()
    }

    fun addClicked() {
        router.openAddTokenSelectChain()
    }

    fun autoEnableTokensChanged(enabled: Boolean) = launch {
        if (enabled == autoEnableTokensRepository.autoEnableTokens()) return@launch

        autoEnableTokens.value = enabled
        autoEnableTokensRepository.setAutoEnableTokens(enabled)
    }

    fun groupClicked(position: Int) = launch {
        val group = itemAt<ManageAssetsRvItem.Group>(position) ?: return@launch

        expandedGroupIds.value = expandedGroupIds.value.toggle(group.key)
    }

    fun groupSwitched(position: Int) = launch {
        val row = itemAt<ManageAssetsRvItem.Group>(position) ?: return@launch
        val group = groupById(row.key) ?: return@launch

        interactor.updateEnabledState(enabled = !group.isEnabled, assetIds = group.allAssetIds())
    }

    fun childSwitched(position: Int) = launch {
        val child = itemAt<ManageAssetsRvItem.Child>(position) ?: return@launch

        interactor.updateEnabledState(enabled = !child.enabled, assetIds = listOf(child.assetId))
    }

    private suspend inline fun <reified T : ManageAssetsRvItem> itemAt(position: Int): T? {
        return listItems.first().getOrNull(position) as? T
    }

    private suspend fun groupById(id: String): ManageAssetGroup? {
        return groupsFlow.first().firstOrNull { it.id == id }
    }
}
