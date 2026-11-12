package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload

class SwapTabFragment : BaseSwapFormFragment() {

    companion object {
        private const val PAYLOAD = "swap_tab_payload"

        fun getBundle(payload: SwapTabPayload?) = Bundle().apply { putParcelable(PAYLOAD, payload) }
    }

    override val display = SwapFormDisplay.TAB

    override fun initialPayload(): SwapSettingsPayload = tabPayload()?.settings ?: SwapSettingsPayload.Restore

    override fun subscribe(viewModel: SwapMainSettingsViewModel) {
        tabPayload()?.let(viewModel::applyTabPayload)
        super.subscribe(viewModel)
    }

    private fun tabPayload(): SwapTabPayload? = argumentOrNull(PAYLOAD)
}
