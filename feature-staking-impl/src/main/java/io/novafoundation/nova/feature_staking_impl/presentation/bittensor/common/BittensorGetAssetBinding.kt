package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetOptionsMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset.GetAssetBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The shared get-asset button, labelled as on iOS ("Swap for TAO", `ui.get.tao.action`) instead of the mixin's "Get TAO".
 * The sheet and its options stay the shared ones.
 */
context(BaseFragment<*, *>)
fun GetAssetOptionsMixin.bindBittensorGetAsset(button: PrimaryButton, label: Flow<String?>) {
    button.setOnClickListener { openAssetOptions() }

    combine(getAssetOptionsButtonState, label) { state, text -> state.withBittensorGetAssetLabel(text) }
        .observe(button::setState)
    observeGetAssetAction.awaitableActionLiveData.observeEvent {
        GetAssetBottomSheet(
            context = requireContext(),
            onCancel = it.onCancel,
            payload = it.payload,
            onClicked = it.onSuccess
        ).show()
    }
}

internal fun DescriptiveButtonState.withBittensorGetAssetLabel(label: String?): DescriptiveButtonState {
    if (label == null) return this
    return when (this) {
        is DescriptiveButtonState.Enabled -> DescriptiveButtonState.Enabled(label)
        is DescriptiveButtonState.Disabled -> DescriptiveButtonState.Disabled(label)
        else -> this
    }
}
