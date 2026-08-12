package io.novafoundation.nova.app.root.presentation

interface RootRouter {

    fun returnToWallet()

    fun nonCancellableVerify()

    fun openUpdateNotifications()

    fun openPushWelcome()

    fun openLegalConsent()

    fun openCloudBackupSettings()

    fun openChainMigrationDetails(chainId: String)
}
