package io.novafoundation.nova.feature_swap_impl.presentation.main

import android.os.Bundle
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload

class SwapMainSettingsFragment : BaseSwapFormFragment() {

    companion object {

        private const val KEY_PAYLOAD = "SwapMainSettingsFragment.payload"

        fun getBundle(payload: SwapSettingsPayload) = Bundle().apply { putParcelable(KEY_PAYLOAD, payload) }
    }

    override val display = SwapFormDisplay.STANDALONE

    override fun initialPayload(): SwapSettingsPayload = argumentOrNull<SwapSettingsPayload>(KEY_PAYLOAD) ?: SwapSettingsPayload.Restore
}
