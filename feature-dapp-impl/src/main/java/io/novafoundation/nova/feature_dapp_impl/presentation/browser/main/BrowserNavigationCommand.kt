package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

sealed class BrowserNavigationCommand {

    object Reload : BrowserNavigationCommand()

    object GoBack : BrowserNavigationCommand()

    class OpenUrl(val url: String) : BrowserNavigationCommand()
}
