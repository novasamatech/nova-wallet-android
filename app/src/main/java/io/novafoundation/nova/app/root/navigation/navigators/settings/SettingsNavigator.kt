package io.novafoundation.nova.app.root.navigation.navigators.settings

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
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
    navigationHolder: SplitScreenNavigationHolder,
    private val rootRouter: RootRouter,
    private val walletConnectDelegate: WalletConnectRouter,
    private val delegate: Navigator
) : BaseNavigator(navigationHolder),
    SettingsRouter {

    override fun returnToWallet() {
        rootRouter.returnToWallet()
    }

    override fun openWallets() {
        delegate.openWallets()
    }

    override fun openNetworks() {
        performNavigation(R.id.action_open_networkManagement)
    }

    override fun openNetworkDetails(payload: ChainNetworkManagementPayload) {
        performNavigation(
            R.id.action_open_networkManagementDetails,
            args = ChainNetworkManagementFragment.getBundle(payload)
        )
    }

    override fun openCustomNode(payload: CustomNodePayload) {
        performNavigation(
            R.id.action_open_customNode,
            args = CustomNodeFragment.getBundle(payload)
        )
    }

    override fun addNetwork() {
        performNavigation(R.id.action_open_preConfiguredNetworks)
    }

    override fun openCreateNetworkFlow() {
        performNavigation(R.id.action_open_addNetworkFragment)
    }

    override fun openCreateNetworkFlow(payload: AddNetworkPayload.Mode.Add) {
        performNavigation(
            R.id.action_open_addNetworkFragment,
            args = AddNetworkMainFragment.getBundle(AddNetworkPayload(payload))
        )
    }

    override fun finishCreateNetworkFlow() {
        performNavigation(R.id.action_finishCreateNetworkFlow, args = NetworkManagementListFragment.getBundle(openAddedTab = true))
    }

    override fun openEditNetwork(payload: AddNetworkPayload.Mode.Edit) {
        performNavigation(
            R.id.action_open_editNetwork,
            args = AddNetworkMainFragment.getBundle(AddNetworkPayload(payload))
        )
    }

    override fun openPushNotificationSettings() {
        performNavigation(R.id.action_open_pushNotificationsSettings)
    }

    override fun openCurrencies() = performNavigation(R.id.action_mainFragment_to_currenciesFragment)

    override fun openLanguages() = performNavigation(R.id.action_mainFragment_to_languagesFragment)

    override fun openAppearance() = performNavigation(R.id.action_mainFragment_to_appearanceFragment)

    override fun openChangePinCode() = performNavigation(
        actionId = R.id.action_change_pin_code,
        args = PincodeFragment.getPinCodeBundle(PinCodeAction.Change)
    )

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
        performNavigation(R.id.action_open_cloudBackupSettings)
    }

    override fun openManualBackup() {
        performNavigation(R.id.action_open_manualBackupSelectWallet)
    }
}
