package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import kotlinx.coroutines.flow.Flow

class RootDeepLinkHandler(
    private val baseHandler: DeepLinkHandler,
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = baseHandler.callbackFlow

    override suspend fun matches(data: Uri): Boolean {
        return baseHandler.matches(data)
    }

    /**
     * We first try to handle our deep links and as a fallback use branch io
     */
    override suspend fun handleDeepLink(data: Uri) {
        baseHandler.handleDeepLink(data)
    }
}
