package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import java.util.Date

class BrowserTab(
    val id: String,
    val metaId: Long,
    val pageSnapshot: PageSnapshot,
    val knownDAppMetadata: KnownDAppMetadata?,
    val currentUrl: String,
    val creationTime: Date
) {

    class KnownDAppMetadata(val iconLink: String)
}
