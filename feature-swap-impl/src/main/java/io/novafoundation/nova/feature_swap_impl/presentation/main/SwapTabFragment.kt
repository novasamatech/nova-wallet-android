package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload

class SwapTabFragment : BaseSwapFormFragment() {

    companion object {
        private const val PAYLOAD = "swap_tab_payload"

        private const val INITIAL_PAYLOAD = "swap_tab_initial_payload"

        fun getBundle(payload: SwapSettingsPayload?) = Bundle().apply { putParcelable(PAYLOAD, payload) }
    }

    override val display = SwapFormDisplay.TAB

    private var creationPayload: SwapSettingsPayload = SwapSettingsPayload.Restore

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && tabPayload() == null) {
            arguments = getBundle(SwapSettingsPayload.Restore)
        }
        creationPayload = savedInstanceState?.getParcelable(INITIAL_PAYLOAD) ?: tabPayload() ?: SwapSettingsPayload.Restore
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Only seeds a new ViewModel after process death; a retained ViewModel keeps the current form.
        outState.putParcelable(INITIAL_PAYLOAD, creationPayload)
        super.onSaveInstanceState(outState)
    }

    override fun initialPayload(): SwapSettingsPayload = creationPayload

    override fun initViews() {
        tabPayload()?.let { payload ->
            arguments?.remove(PAYLOAD)
            viewModel.applyTabPayload(payload)
        }
        super.initViews()
    }

    private fun tabPayload(): SwapSettingsPayload? = argumentOrNull(PAYLOAD)
}
