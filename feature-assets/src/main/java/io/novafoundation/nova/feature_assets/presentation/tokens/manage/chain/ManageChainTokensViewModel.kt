package io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.MultiChainToken
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.model.ChainTokenInstanceModel
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.MultiChainTokenMapper
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ManageChainTokensViewModel(
    private val interactor: ManageTokenInteractor,
    private val commonUiMapper: MultiChainTokenMapper,
    payload: ManageChainTokensPayload,
) : BaseViewModel() {

    private val multiChainTokensFlow = interactor.multiChainTokenFlow(payload.multiChainTokenId)
        .shareInBackground()

    val headerModel = multiChainTokensFlow
        .map(commonUiMapper::mapHeaderToUi)
        .shareInBackground()

    val chainInstanceModels = multiChainTokensFlow
        .map(::mapMultiChainTokenToInstanceModels)
        .shareInBackground()

    fun enableChainSwitchClicked(position: Int) = launch {
        val chainTokenInstance = multiChainTokensFlow.first().instances.getOrNull(position) ?: return@launch
        val assetId = FullChainAssetId(chainTokenInstance.chain.id, chainTokenInstance.chainAssetId)

        interactor.updateEnabledState(enabled = !chainTokenInstance.isEnabled, assetIds = listOf(assetId))
    }

    private fun mapMultiChainTokenToInstanceModels(multiChainToken: MultiChainToken): List<ChainTokenInstanceModel> {
        return multiChainToken.instances.map {
            ChainTokenInstanceModel(
                chainUi = mapChainToUi(it.chain),
                enabled = it.isEnabled,
                switchable = it.isSwitchable
            )
        }
    }
}
