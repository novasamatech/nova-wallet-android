package io.novafoundation.nova.feature_settings_impl

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodePayload

interface SettingsRouter : ReturnableRouter {

    fun openWallets()

    fun openNetworks()

    fun openNetworkDetails(payload: ChainNetworkManagementPayload)

    fun openCustomNode(payload: CustomNodePayload)

    fun openPushNotificationSettings()

    fun openCurrencies()

    fun openLanguages()

    fun openChangePinCode()

    fun openWalletDetails(metaId: Long)

    fun openSwitchWallet()

    fun openWalletConnectScan()

    fun openWalletConnectSessions()

    fun openCloudBackupSettings()

    fun openManualBackup()

    fun addNetwork()
}
