package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardState
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InitialTopUpCase(
    private val assetsRouter: AssetsRouter,
    private val resourceManager: ResourceManager,
    private val novaCardInteractor: NovaCardInteractor
) : TopUpCase {

    override val titleFlow: Flow<String> = flowOf { resourceManager.getString(R.string.waiting_initial_top_up_card_title) }

    override fun init(viewModel: BaseViewModel) {
        novaCardInteractor.observeNovaCardState()
            .onEach { novaCardState ->
                if (novaCardState == NovaCardState.CREATED) {
                    assetsRouter.back()
                }
            }.launchIn(viewModel)
    }

    override fun onTimeFinished(viewModel: BaseViewModel) {
        novaCardInteractor.setNovaCardState(NovaCardState.NONE)

        viewModel.showError(
            resourceManager.getString(R.string.fragment_waiting_top_up_time_out_error_title),
            resourceManager.getString(R.string.fragment_waiting_top_up_time_out_error_message)
        )
    }
}
