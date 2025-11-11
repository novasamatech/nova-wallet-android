package io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.setState

context(BaseFragment<*, *>)
fun GetAssetOptionsMixin.bindGetAsset(
    button: PrimaryButton
) {
    button.setOnClickListener { openAssetOptions() }

    getAssetOptionsButtonState.observe(button::setState)
    observeGetAssetAction.awaitableActionLiveData.observeEvent {
        GetAssetBottomSheet(
            context = requireContext(),
            onCancel = it.onCancel,
            payload = it.payload,
            onClicked = it.onSuccess
        ).show()
    }
}
