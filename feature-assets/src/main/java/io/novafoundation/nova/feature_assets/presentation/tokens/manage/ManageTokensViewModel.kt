package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken
import io.novafoundation.nova.feature_assets.domain.tokens.manage.allChainAssetIds
import io.novafoundation.nova.feature_assets.domain.tokens.manage.isEnabled
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenMapper
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ManageTokensViewModel(
    private val router: AssetsRouter,
    private val interactor: ManageTokenInteractor,
    private val commonUiMapper: MultiChainTokenMapper
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
}
