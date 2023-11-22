package io.novafoundation.nova.app.root.presentation.deepLinks

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
        nestedHandlers.forEach { handler ->
            if (handler.matches(data)) {
                handler.handleDeepLink(data)
                return
            }
        }
    }
}
