package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class EmptyTopUpCase(
    private val assetsRouter: AssetsRouter
) : TopUpCase {

    override val titleFlow: Flow<String> = emptyFlow()

    override fun init(viewModel: BaseViewModel) {
        assetsRouter.back()
    }

    override fun onTimeFinished(viewModel: BaseViewModel) {}
}
