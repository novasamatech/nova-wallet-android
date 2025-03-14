package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TopUpExistingCardCase(
    private val assetsRouter: AssetsRouter,
    private val resourceManager: ResourceManager,
    private val novaCardInteractor: NovaCardInteractor
) : TopUpCase {

    override val titleFlow: Flow<String> = flowOf { resourceManager.getString(R.string.waiting_existing_card_top_up_card_title) }

    override fun init(viewModel: BaseViewModel) {
        novaCardInteractor.observeTopUpFinishedEvent()
            .onEach { assetsRouter.back() }
            .launchIn(viewModel)
    }

    override fun onTimeFinished(viewModel: BaseViewModel) {
        // Do nothing
    }
}
