package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val DAPP_DEEP_LINK_PREFIX = "/open/dapp"

class DAppDeepLinkHandler(
    private val accountRepository: AccountRepository,
    private val dappRepository: DAppMetadataRepository,
    private val dAppRouter: DAppRouter
) : DeepLinkHandler {

    override val callbackFlow: Flow<CallbackEvent> = emptyFlow()

    override suspend fun matches(data: Uri): Boolean {
        val path = data.path ?: return false
        return path.startsWith(DAPP_DEEP_LINK_PREFIX)
    }

    override suspend fun handleDeepLink(data: Uri) {
        // TODO: check that user has accounts here
        val url = data.getDappUrl() ?: return
        val normalizedUrl = Urls.normalizeUrl(url)

        val dAppMetadata = dappRepository.syncAndGetDapp(normalizedUrl)
        if (dAppMetadata == null) return // TODO: handle error
        dAppRouter.openDAppBrowser(normalizedUrl)
    }

    private fun Uri.getDappUrl(): String? {
        return getQueryParameter("url")
    }
}
