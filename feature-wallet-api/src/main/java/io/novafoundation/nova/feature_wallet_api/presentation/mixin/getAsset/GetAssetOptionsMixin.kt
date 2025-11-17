package io.novafoundation.nova.feature_wallet_api.presentation.mixin.getAsset

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.feature_wallet_api.domain.model.GetAssetOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GetAssetOptionsMixin {

    interface Factory {
        fun create(
            assetFlow: Flow<Chain.Asset?>,
            scope: CoroutineScope,
            additionalButtonFilter: Flow<Boolean> = flowOf(true)
        ): GetAssetOptionsMixin
    }

    val getAssetOptionsButtonState: Flow<DescriptiveButtonState>

    val observeGetAssetAction: ActionAwaitableMixin.Presentation<GetAssetBottomSheet.Payload, GetAssetOption>

    fun openAssetOptions()
}
