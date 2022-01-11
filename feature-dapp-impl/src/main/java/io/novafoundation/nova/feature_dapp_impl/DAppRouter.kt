package io.novafoundation.nova.feature_dapp_impl

interface DAppRouter {

    fun openChangeAccount()

    fun openDAppBrowser(initialUrl: String)

    fun openDappSearch()

    fun openExtrinsicDetails(extrinsicContent: String)

    fun back()
}
