package io.novafoundation.nova.feature_dapp_impl.presentation.tab

data class BrowserTabRvItem(
    val tabId: String,
    val tabName: String?,
    val knownDappIconUrl: String?,
    val tabFaviconPath: String?,
    val tabScreenshotPath: String?,
)
