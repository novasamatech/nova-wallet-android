package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

sealed class BrowserCommand {

    object Reload : BrowserCommand()

    object GoBack : BrowserCommand()

    class OpenUrl(val url: String) : BrowserCommand()

    class ChangeDesktopMode(val enabled: Boolean) : BrowserCommand()
}
