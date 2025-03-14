package io.novafoundation.nova.feature_assets.presentation.novacard.waiting.flows

import io.novafoundation.nova.common.base.BaseViewModel
import kotlinx.coroutines.flow.Flow

interface TopUpCase {

    val titleFlow: Flow<String>

    fun init(viewModel: BaseViewModel)

    fun onTimeFinished(viewModel: BaseViewModel)
}
