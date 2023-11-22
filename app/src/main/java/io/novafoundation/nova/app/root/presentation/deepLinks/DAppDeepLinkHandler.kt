package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.model.DappMetadata
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
        val userAccounts = accountRepository.allMetaAccounts()
        val path = data.path ?: return false
        val url = data.getDappUrl() ?: return false
        val dappInfo = syncAndGetDapp(url)

        return userAccounts.isNotEmpty() &&
            path.startsWith(DAPP_DEEP_LINK_PREFIX) &&
            dappInfo != null
    }

    override suspend fun handleDeepLink(data: Uri) {
        val url = data.getDappUrl() ?: return
        dAppRouter.openDAppBrowser(url)
    }

    private fun Uri.getDappUrl(): String? {
        return getQueryParameter("url")
    }

    private suspend fun syncAndGetDapp(url: String): DappMetadata? {
        dappRepository.syncDAppMetadatas()

        val normalizedUrl = Urls.normalizeUrl(url)
        return dappRepository.getDAppMetadata(normalizedUrl)
    }
}
