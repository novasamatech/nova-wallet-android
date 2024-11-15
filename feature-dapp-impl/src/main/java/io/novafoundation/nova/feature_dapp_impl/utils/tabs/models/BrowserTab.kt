package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import java.util.Date

class BrowserTab(
    val id: String,
    val pageSnapshot: PageSnapshot,
    val currentUrl: String,
    val creationTime: Date
)
