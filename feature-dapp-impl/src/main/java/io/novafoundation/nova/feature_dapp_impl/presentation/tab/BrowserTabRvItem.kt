package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import io.novafoundation.nova.common.utils.images.Icon

data class BrowserTabRvItem(
    val tabId: String,
    val tabName: String?,
    val icon: Icon?,
    val tabScreenshotPath: String?,
)
