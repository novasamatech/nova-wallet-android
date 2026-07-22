package io.novafoundation.nova.app.root.presentation

interface RootRouter {

    fun returnToWallet()

    fun nonCancellableVerify()

    fun openUpdateNotifications()

    fun openConsentBannerUpgrade()

    fun openPushWelcome()

    fun openCloudBackupSettings()

    fun openChainMigrationDetails(chainId: String)
}
