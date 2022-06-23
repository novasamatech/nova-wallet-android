package io.novafoundation.nova.feature_assets.presentation.send.confirm.hints

import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.isCrossChain
import kotlinx.coroutines.CoroutineScope

class ConfirmSendHintsMixinFactory(
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    private val transferDraft: TransferDraft,
) {

    fun create(scope: CoroutineScope) = resourcesHintsMixinFactory.create(
        coroutineScope = scope,
        hintsRes = if (transferDraft.isCrossChain) {
            listOf(R.string.wallet_send_confirm_hint)
        } else {
            emptyList()
        }
    )
}
