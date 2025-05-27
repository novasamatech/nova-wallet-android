package io.novafoundation.nova.feature_buy_impl.presentation.deeplink

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.ProviderUtils
import io.novafoundation.nova.feature_deep_linking.R
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import kotlinx.coroutines.flow.MutableSharedFlow

class BuyCallbackDeepLinkHandler(
    private val resourceManager: ResourceManager
) : DeepLinkHandler {

    override val callbackFlow: MutableSharedFlow<CallbackEvent> = singleReplaySharedFlow()

    override suspend fun matches(data: Uri): Boolean {
        val link = data.toString()
        return ProviderUtils.REDIRECT_URL_BASE in link
    }

    override suspend fun handleDeepLink(data: Uri) = runCatching {
        val message = resourceManager.getString(R.string.buy_completed)
        callbackFlow.emit(CallbackEvent.Message(message))
    }
}
