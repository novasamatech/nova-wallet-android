package io.novafoundation.nova.app.root.navigation.navigators.settings

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsPayload
import io.novafoundation.nova.feature_push_notifications.presentation.settings.default
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkMainFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.chain.ChainNetworkManagementPayload
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main.NetworkManagementListFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodeFragment
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node.CustomNodePayload
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

class SettingsNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val rootRouter: RootRouter,
    private val walletConnectDelegate: WalletConnectRouter,
    private val delegate: Navigator
) : BaseNavigator(navigationHoldersRegistry),
    SettingsRouter {

    override fun returnToWallet() {
        rootRouter.returnToWallet()
    }

    override fun openWallets() {
        delegate.openWallets()
    }

    override fun openNetworks() {
        navigationBuilder().action(R.id.action_open_networkManagement)
            .navigateInFirstAttachedContext()
    }

    override fun openNetworkDetails(payload: ChainNetworkManagementPayload) {
        navigationBuilder().action(R.id.action_open_networkManagementDetails)
            .setArgs(ChainNetworkManagementFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openCustomNode(payload: CustomNodePayload) {
        navigationBuilder().action(R.id.action_open_customNode)
            .setArgs(CustomNodeFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun addNetwork() {
        navigationBuilder().action(R.id.action_open_preConfiguredNetworks)
            .navigateInFirstAttachedContext()
    }

    override fun openCreateNetworkFlow() {
        navigationBuilder().action(R.id.action_open_addNetworkFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openCreateNetworkFlow(payload: AddNetworkPayload.Mode.Add) {
        navigationBuilder().action(R.id.action_open_addNetworkFragment)
            .setArgs(AddNetworkMainFragment.getBundle(AddNetworkPayload(payload)))
            .navigateInFirstAttachedContext()
    }

    override fun finishCreateNetworkFlow() {
        navigationBuilder().action(R.id.action_finishCreateNetworkFlow)
            .setArgs(NetworkManagementListFragment.getBundle(openAddedTab = true))
            .navigateInFirstAttachedContext()
    }

    override fun openEditNetwork(payload: AddNetworkPayload.Mode.Edit) {
        navigationBuilder().action(R.id.action_open_editNetwork)
            .setArgs(AddNetworkMainFragment.getBundle(AddNetworkPayload(payload)))
            .navigateInFirstAttachedContext()
    }

    override fun openPushNotificationSettings() {
        navigationBuilder().action(R.id.action_open_pushNotificationsSettings)
            .setArgs(PushSettingsFragment.createPayload(PushSettingsPayload.default()))
            .navigateInFirstAttachedContext()
    }

    override fun openCurrencies() {
        navigationBuilder().action(R.id.action_mainFragment_to_currenciesFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openLanguages() {
        navigationBuilder().action(R.id.action_mainFragment_to_languagesFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openAppearance() {
        navigationBuilder().action(R.id.action_mainFragment_to_appearanceFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChangePinCode() {
        navigationBuilder().action(R.id.action_change_pin_code)
            .setArgs(PincodeFragment.getPinCodeBundle(PinCodeAction.Change))
            .navigateInFirstAttachedContext()
    }

    override fun openWalletDetails(metaId: Long) {
        delegate.openWalletDetails(metaId)
    }

    override fun openSwitchWallet() {
        delegate.openSwitchWallet()
    }

    override fun openWalletConnectScan() {
        walletConnectDelegate.openScanPairingQrCode()
    }

    override fun openWalletConnectSessions() {
        walletConnectDelegate.openWalletConnectSessions(WalletConnectSessionsPayload(metaId = null))
    }

    override fun openCloudBackupSettings() {
        navigationBuilder().action(R.id.action_open_cloudBackupSettings)
            .navigateInFirstAttachedContext()
    }

    override fun openManualBackup() {
        navigationBuilder().action(R.id.action_open_manualBackupSelectWallet)
            .navigateInFirstAttachedContext()
    }
}
