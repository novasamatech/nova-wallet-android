package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class RootDeepLinkHandler(
    private val nestedHandlers: Collection<DeepLinkHandler>,
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = nestedHandlers
        .mapNotNull { it.callbackFlow }
        .merge()

    override suspend fun matches(data: Uri): Boolean {
        return nestedHandlers.any { it.matches(data) }
    }

    override suspend fun handleDeepLink(data: Uri) {
        nestedHandlers.find { it.matches(data) }
            ?.handleDeepLink(data)
    }
}
