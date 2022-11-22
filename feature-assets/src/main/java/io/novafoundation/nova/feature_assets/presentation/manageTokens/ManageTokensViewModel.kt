package io.novafoundation.nova.feature_assets.presentation.manageTokens

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.manageTokens.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.manageTokens.MultiChainToken
import io.novafoundation.nova.feature_assets.domain.manageTokens.allChainAssetIds
import io.novafoundation.nova.feature_assets.domain.manageTokens.isEnabled
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.manageTokens.model.MultiChainTokenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    private val multiChainTokensFlow = interactor.multiChainTokensFlow(query)
        .shareInBackground()

    val searchResults = multiChainTokensFlow
        .mapList(::mapMultiChainTokenToUi)
        .shareInBackground()

    fun closeClicked() {
        router.back()
    }

    fun addClicked() {
        showMessage("TODO open add")
    }

    fun editClicked() {
        showMessage("TODO open edit")
    }

    fun enableTokenSwitchClicked(position: Int) = launch {
        val token = multiChainTokensFlow.first().getOrNull(position) ?: return@launch

        interactor.updateEnabledState(
            enabled = !token.isEnabled(),
            assetIds = token.allChainAssetIds()
        )
    }

    private fun mapMultiChainTokenToUi(multiChainToken: MultiChainToken): MultiChainTokenModel {
        return MultiChainTokenModel(
            icon = multiChainToken.icon,
            symbol = multiChainToken.symbol,
            networks = constructNetworksSubtitle(multiChainToken),
            enabled = multiChainToken.isEnabled()
        )
    }

    private fun constructNetworksSubtitle(multiChainToken: MultiChainToken): String {
        val enabledInstances = multiChainToken.instances.filter { it.isEnabled }

        return when (enabledInstances.size) {
            0 -> resourceManager.getString(R.string.common_disabled)

            multiChainToken.instances.size -> resourceManager.getString(R.string.assets_manage_tokens_all_networks)

            1 -> enabledInstances.single().chain.name

            else -> {
                val firstChain = enabledInstances.first()
                val othersCount = enabledInstances.size - 1

                resourceManager.getString(
                    R.string.assets_manage_tokens_partial_networks,
                    firstChain.chainAssetId,
                    othersCount.format()
                )
            }
        }
    }
}
