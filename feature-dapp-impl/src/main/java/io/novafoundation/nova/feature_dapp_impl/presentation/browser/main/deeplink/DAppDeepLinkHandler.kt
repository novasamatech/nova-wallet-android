package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.deeplink

import android.net.Uri
import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.DAppHandlingException
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val DAPP_DEEP_LINK_PREFIX = "/open/dapp"

class DAppDeepLinkHandler(
    private val dappRepository: DAppMetadataRepository,
    private val router: DAppRouter,
    private val automaticInteractionGate: AutomaticInteractionGate
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false
        return path.startsWith(DAPP_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) {
        automaticInteractionGate.awaitInteractionAllowed()

        val url = data.getDappUrl() ?: throw DAppHandlingException.UrlIsInvalid
        val normalizedUrl = runCatching { Urls.normalizeUrl(url) }.getOrNull() ?: throw DAppHandlingException.UrlIsInvalid

        ensureDAppInCatalog(normalizedUrl)

        router.openDAppBrowser(DAppBrowserPayload.Address(url))
    }

    private suspend fun ensureDAppInCatalog(normalizedUrl: String) {
        dappRepository.syncAndGetDapp(normalizedUrl)
            ?: throw DAppHandlingException.DomainIsNotMatched(normalizedUrl)
    }

    private fun Uri.getDappUrl(): String? {
        return getQueryParameter("url")
    }
}
