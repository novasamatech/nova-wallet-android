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

    private val availableChains = interactor.availableChainsToAddTokenFlow()
        .shareInBackground()

    val availableChainModels = availableChains
        .mapList(::mapChainToUi)
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun chainClicked(position: Int) = launch {
        val chain = getChainAt(position) ?: return@launch

        val payload = AddTokenEnterInfoPayload(chain.id)
        router.openAddTokenEnterInfo(payload)
    }

    private suspend fun getChainAt(position: Int): Chain? {
        return availableChains.first().getOrNull(position)
    }
}
