package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddTokenSelectChainViewModel(
    private val router: AssetsRouter,
    private val interactor: AddTokensInteractor,
) : BaseViewModel() {

    private val availableEthereumChains = interactor.availableChainsToAddErc20TokenFlow()
        .shareInBackground()

    private val availableSubstrateChains = interactor.availableChainsToAddSubstrateTokenFlow()
        .shareInBackground()

    val availableEthereumChainModels = availableEthereumChains
        .mapList(::mapChainToUi)
        .shareInBackground()

    val availableSubstrateChainModels = availableSubstrateChains
        .mapList(::mapChainToUi)
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun chainClicked(
        position: Int,
        isEthereumBased: Boolean
    ) = launch {
        val chain = if (isEthereumBased) {
            getEthereumChainAt(position)
        } else {
            getSubstrateChainAt(position)
        }  ?: return@launch

        val payload = AddTokenEnterInfoPayload(chain.id)
        router.openAddTokenEnterInfo(payload)
    }

    private suspend fun getEthereumChainAt(position: Int): Chain? {
        return availableEthereumChains.first().getOrNull(position)
    }

    private suspend fun getSubstrateChainAt(position: Int): Chain? {
        return availableSubstrateChains.first().getOrNull(position)
    }
}
