package io.novafoundation.nova.app.root.presentation

import io.novafoundation.nova.common.navigation.DelayedNavigation

interface RootRouter {

    fun returnToWallet()

    fun nonCancellableVerify()

    fun openUpdateNotifications()

    fun openPushWelcome()

    fun openLegalConsent()

    /** Continues where the PIN was taking the user before the consent screen stepped in. */
    fun finishAnalyticsConsent(next: DelayedNavigation)

    fun openCloudBackupSettings()

    fun openChainMigrationDetails(chainId: String)
}
