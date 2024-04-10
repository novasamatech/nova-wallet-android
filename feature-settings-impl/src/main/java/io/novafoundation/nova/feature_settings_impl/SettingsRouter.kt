package io.novafoundation.nova.feature_settings_impl

interface SettingsRouter {

    fun openWallets()

    fun openPushNotificationSettings()

    fun openCurrencies()

    fun openLanguages()

    fun openChangePinCode()

    fun openWalletDetails(metaId: Long)

    fun openSwitchWallet()

    fun openWalletConnectScan()

    fun openWalletConnectSessions()

    fun openCloudBackupSettings()
}
