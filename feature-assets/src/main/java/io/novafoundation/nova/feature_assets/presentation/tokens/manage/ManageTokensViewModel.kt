package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.repository.AssetsViewModeRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.AutoEnableTokensRepository
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageAssetGroup
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.allAssetIds
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsMapper
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageAssetsRvItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val mapper: ManageAssetsMapper,
    private val autoEnableTokensRepository: AutoEnableTokensRepository,
    private val accountRepository: AccountRepository,
    assetsViewModeRepository: AssetsViewModeRepository,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    /**
     * Read straight from storage rather than seeded with a guess: a placeholder would reach the
     * switch before the real value did, and the switch cannot tell being set apart from being
     * tapped - so the guess would be written back as if the user had chosen it.
     */
    val autoEnableTokens = accountRepository.selectedMetaAccountFlow()
        .flatMapLatest { autoEnableTokensRepository.autoEnableTokensFlow(it.id) }
        .shareInBackground()

    private val expandedGroupIds = MutableStateFlow(emptySet<String>())

    private val groupsFlow = interactor.assetGroupsFlow(query, assetsViewModeRepository.assetsViewModeFlow())
        .shareInBackground()

    val listItems = combine(groupsFlow, expandedGroupIds, query) { groups, expanded, currentQuery ->
        mapper.mapGroupsToUi(groups, expanded, searching = currentQuery.isNotEmpty())
    }.shareInBackground()

    /**
     * Off means we stop following tokens that are not on screen, so a balance arriving in one of
     * them can no longer reveal it. It is a traffic setting, not a display one.
     */
    fun autoEnableTokensChanged(enabled: Boolean) = launch {
        autoEnableTokensRepository.setAutoEnableTokens(accountRepository.getSelectedMetaAccount().id, enabled)
    }

    fun closeClicked() {
        router.back()
    }

    fun addClicked() {
        router.openAddTokenSelectChain()
    }

    fun groupClicked(position: Int) = launch {
        val group = itemAt<ManageAssetsRvItem.Group>(position) ?: return@launch

        expandedGroupIds.value = expandedGroupIds.value.toggle(group.key)
    }

    fun groupSwitched(position: Int) = launch {
        val row = itemAt<ManageAssetsRvItem.Group>(position) ?: return@launch
        val group = groupById(row.key) ?: return@launch

        interactor.updateVisibility(visible = !group.isEnabled, assetIds = group.allAssetIds())
    }

    fun childSwitched(position: Int) = launch {
        val child = itemAt<ManageAssetsRvItem.Child>(position) ?: return@launch

        interactor.updateVisibility(visible = !child.enabled, assetIds = listOf(child.assetId))
    }

    private suspend inline fun <reified T : ManageAssetsRvItem> itemAt(position: Int): T? {
        return listItems.first().getOrNull(position) as? T
    }

    private suspend fun groupById(id: String): ManageAssetGroup? {
        return groupsFlow.first().firstOrNull { it.id == id }
    }
}
