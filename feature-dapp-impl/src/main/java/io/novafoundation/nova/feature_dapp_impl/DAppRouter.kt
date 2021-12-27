package io.novafoundation.nova.feature_dapp_impl

interface DAppRouter {

    fun openChangeAccount()

    fun openDAppBrowser()

    fun openExtrinsicDetails(extrinsicContent: String)

    fun back()
}
