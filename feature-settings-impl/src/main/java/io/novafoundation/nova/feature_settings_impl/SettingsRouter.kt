package io.novafoundation.nova.feature_settings_impl

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface SettingsRouter : ReturnableRouter {

    fun openWallets()

    fun openNetworks()

    fun openPushNotificationSettings()

    fun openCurrencies()

    fun openLanguages()

    fun openChangePinCode()

    fun openWalletDetails(metaId: Long)

    fun openSwitchWallet()

    fun openWalletConnectScan()

    fun openWalletConnectSessions()
}
