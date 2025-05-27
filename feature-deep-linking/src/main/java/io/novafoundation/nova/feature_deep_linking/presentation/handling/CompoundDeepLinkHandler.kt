package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class CompoundDeepLinkHandler(
    private val nestedHandlers: Collection<DeepLinkHandler>
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = nestedHandlers
        .mapNotNull { it.callbackFlow }
        .merge()

    override suspend fun matches(data: Uri): Boolean {
        return nestedHandlers.any { it.matches(data) }
    }

    override suspend fun handleDeepLink(data: Uri) {
        val firstHandler = nestedHandlers.find { it.canHandle(data) }

        firstHandler?.handleDeepLink(data)
    }

    private suspend fun DeepLinkHandler.canHandle(data: Uri) = runCatching { this.matches(data) }
        .getOrDefault(false)

}
