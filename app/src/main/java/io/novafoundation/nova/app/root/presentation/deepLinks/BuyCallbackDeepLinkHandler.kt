package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

class BuyCallbackDeepLinkHandler(
    private val interactor: RootInteractor,
    private val resourceManager: ResourceManager
) : DeepLinkHandler {

    override val callbackFlow: MutableSharedFlow<CallbackEvent> = singleReplaySharedFlow()

    override suspend fun matches(data: Uri): Boolean {
        return interactor.isBuyProviderRedirectLink(data.toString())
    }

    override suspend fun handleDeepLink(data: Uri) {
        val message = resourceManager.getString(R.string.buy_completed)
        callbackFlow.emit(CallbackEvent.Message(message))
    }
}
