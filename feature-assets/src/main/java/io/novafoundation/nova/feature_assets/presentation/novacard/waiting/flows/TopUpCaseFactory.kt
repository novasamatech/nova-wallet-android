package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

class TopUpCaseFactory(
    private val assetsRouter: AssetsRouter,
    private val resourceManager: ResourceManager,
    private val novaCardInteractor: NovaCardInteractor
) {
    fun create(state: NovaCardState): TopUpCase {
        return when (state) {
            NovaCardState.NONE -> EmptyTopUpCase(assetsRouter)
            NovaCardState.CREATION -> InitialTopUpCase(
                assetsRouter,
                resourceManager,
                novaCardInteractor
            )

            NovaCardState.CREATED -> TopUpExistingCardCase(
                assetsRouter,
                resourceManager,
                novaCardInteractor
            )
        }
    }
}
