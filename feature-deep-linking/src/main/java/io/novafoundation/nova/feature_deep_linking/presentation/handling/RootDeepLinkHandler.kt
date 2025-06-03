package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import io.novafoundation.nova.common.utils.onFailureInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class RootDeepLinkHandler(
    private val pendingDeepLinkProvider: PendingDeepLinkProvider,
    private val nestedHandlers: Collection<DeepLinkHandler>
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = nestedHandlers
        .mapNotNull { it.callbackFlow }
        .merge()

    override suspend fun matches(data: Uri): Boolean {
        return nestedHandlers.any { it.matches(data) }
    }

    suspend fun checkAndHandlePendingDeepLink(): Result<Unit> {
        val pendingDeepLink = pendingDeepLinkProvider.get() ?: return Result.failure(IllegalStateException("No pending deep link found"))

        return handleDeepLinkInternal(pendingDeepLink)
            .onSuccess { pendingDeepLinkProvider.clear() }
    }

    override suspend fun handleDeepLink(data: Uri): Result<Unit> {
        pendingDeepLinkProvider.save(data)
        return handleDeepLinkInternal(data)
            .onSuccess { pendingDeepLinkProvider.clear() }
            .onFailureInstance<HandlerNotFoundException, Unit> { pendingDeepLinkProvider.clear() } // If we haven't find any handler - no need to save deep link
    }

    private suspend fun handleDeepLinkInternal(data: Uri): Result<Unit> {
        val firstHandler = nestedHandlers.find { it.canHandle(data) } ?: return Result.failure(HandlerNotFoundException())

        return firstHandler.handleDeepLink(data)
    }

    private suspend fun DeepLinkHandler.canHandle(data: Uri) = runCatching { this.matches(data) }
        .getOrDefault(false)
}

private class HandlerNotFoundException : Exception()
