package io.novafoundation.nova.common.utils.browser.tabs.models

import java.util.Date

class BrowserTab(
    val id: String,
    val pageSnapshot: PageSnapshot,
    val currentUrl: String,
    val creationTime: Date
)
