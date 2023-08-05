package io.novafoundation.nova.feature_settings_impl

interface SettingsRouter {

    fun openWallets()

    fun openNftList()

    fun openCurrencies()

    fun openLanguages()

    fun openChangePinCode()

    fun openAccountDetails(metaId: Long)

    fun openSwitchWallet()

    fun openWalletConnectScan()

    fun openWalletConnectSessions()
}
