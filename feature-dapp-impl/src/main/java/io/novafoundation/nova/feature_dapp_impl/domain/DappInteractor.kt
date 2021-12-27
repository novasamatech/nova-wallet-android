package io.novafoundation.nova.feature_dapp_impl.domain

import io.novafoundation.nova.feature_dapp_impl.domain.browser.DAppInfo
import io.novafoundation.nova.feature_dapp_impl.util.UrlNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DappInteractor {

    suspend fun getDAppInfo(dAppUrl: String): DAppInfo = withContext(Dispatchers.Default) {
        DAppInfo(
            baseUrl = UrlNormalizer.normalizeUrl(dAppUrl),
            metadata = null // TODO whitelist task
        )
    }
}
