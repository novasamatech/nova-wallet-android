package io.novafoundation.nova.app.root.navigation.navigators

import android.os.Bundle
import androidx.lifecycle.asFlow
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.delayedNavigation.BackDelayedNavigation
import io.novafoundation.nova.app.root.navigation.delayedNavigation.NavComponentDelayedNavigation
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.utils.getParcelableCompat
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionFragment
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionModePayload
import io.novafoundation.nova.feature_account_impl.presentation.account.details.WalletDetailsFragment
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateBackupPasswordPayload
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateWalletBackupPasswordFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ExportJsonFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedFragment
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.common.ManualBackupCommonPayload
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.advanced.ManualBackupAdvancedSecretsFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.main.ManualBackupSecretsFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.warning.ManualBackupWarningFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.node.details.NodeDetailsFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerStartPayload
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.finish.FinishImportParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview.PreviewImportParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan.ScanImportParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start.StartImportParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.ScanSignParitySignerFragment
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.scan.model.ScanSignParitySignerPayload
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import io.novafoundation.nova.feature_account_impl.presentation.pincode.ToolbarConfiguration
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletFragment
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletPayload.FlowType
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.ChangeWatchAccountFragment
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendFragment
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.send.confirm.ConfirmSendFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.ExtrinsicDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct.RewardDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool.PoolRewardDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.swap.SwapDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.transfer.TransferDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.TransactionHistoryFilterPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.ConfirmContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms.MoonbeamCrowdloanTermsFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.CrowdloanContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.AddChainAccountSelectLedgerFragment
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.WelcomeFragment
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsFragment
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload
import io.novafoundation.nova.splash.SplashRouter
import kotlinx.coroutines.flow.Flow

class Navigator(
    rootNavigationHolder: RootNavigationHolder,
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    private val walletConnectDelegate: WalletConnectRouter,
    private val stakingDashboardDelegate: StakingDashboardRouter
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder),
    SplashRouter,
    OnboardingRouter,
    AccountRouter,
    AssetsRouter,
    RootRouter,
    CrowdloanRouter {

    override fun openWelcomeScreen() {
        navigationBuilder()
            .addCase(R.id.accountsFragment, R.id.action_walletManagment_to_welcome)
            .addCase(R.id.splashFragment, R.id.action_splash_to_onboarding)
            .setArgs(WelcomeFragment.bundle(false))
            .perform()
    }

    override fun openInitialCheckPincode() {
        val action = PinCodeAction.Check(NavComponentDelayedNavigation(R.id.action_open_split_screen), ToolbarConfiguration())

        navigationBuilder(R.id.action_splash_to_pin)
            .setArgs(PincodeFragment.getPinCodeBundle(action))
            .perform()
    }

    override fun openCreateFirstWallet() {
        navigationBuilder(R.id.action_welcomeFragment_to_startCreateWallet)
            .setArgs(StartCreateWalletFragment.bundle(StartCreateWalletPayload(FlowType.FIRST_WALLET)))
            .perform()
    }

    override fun openMain() {
        navigationBuilder(R.id.action_open_split_screen)
            .perform()
    }

    override fun openAfterPinCode(delayedNavigation: DelayedNavigation) {
        when (delayedNavigation) {
            is NavComponentDelayedNavigation -> {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.pincodeFragment, true)
                    .setEnterAnim(R.anim.fragment_open_enter)
                    .setExitAnim(R.anim.fragment_open_exit)
                    .setPopEnterAnim(R.anim.fragment_close_enter)
                    .setPopExitAnim(R.anim.fragment_close_exit)
                    .build()

                navigationBuilder(delayedNavigation.globalActionId)
                    .setArgs(delayedNavigation.extras)
                    .setNavOptions(navOptions)
                    .perform()
            }

            is BackDelayedNavigation -> back()
        }
    }

    override fun openCreatePincode() {
        val args = buildCreatePinBundle()

        navigationBuilder()
            .addCase(R.id.splashFragment, R.id.action_splash_to_pin)
            .addCase(R.id.importAccountFragment, R.id.action_importAccountFragment_to_pincodeFragment)
            .addCase(R.id.confirmMnemonicFragment, R.id.action_confirmMnemonicFragment_to_pincodeFragment)
            .addCase(R.id.createWatchWalletFragment, R.id.action_watchWalletFragment_to_pincodeFragment)
            .addCase(R.id.finishImportParitySignerFragment, R.id.action_finishImportParitySignerFragment_to_pincodeFragment)
            .addCase(R.id.finishImportLedgerFragment, R.id.action_finishImportLedgerFragment_to_pincodeFragment)
            .addCase(R.id.createCloudBackupPasswordFragment, R.id.action_createCloudBackupPasswordFragment_to_pincodeFragment)
            .addCase(R.id.restoreCloudBackupFragment, R.id.action_restoreCloudBackupFragment_to_pincodeFragment)
            .addCase(R.id.finishImportGenericLedgerFragment, R.id.action_finishImportGenericLedgerFragment_to_pincodeFragment)
            .setArgs(args)
            .perform()
    }

    override fun openAdvancedSettings(payload: AdvancedEncryptionModePayload) {
        navigationBuilder(R.id.action_open_advancedEncryptionFragment)
            .setArgs(AdvancedEncryptionFragment.getBundle(payload))
            .perform()
    }

    override fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload) {
        navigationBuilder(R.id.action_backupMnemonicFragment_to_confirmMnemonicFragment)
            .setArgs(ConfirmMnemonicFragment.getBundle(confirmMnemonicPayload))
            .perform()
    }

    override fun openImportAccountScreen(payload: ImportAccountPayload) {
        navigationBuilder()
            .addCase(R.id.splashFragment, R.id.action_splashFragment_to_import_nav_graph)
            .setFallbackCase(R.id.action_import_nav_graph)
            .setArgs(ImportAccountFragment.getBundle(payload))
            .perform()
    }

    override fun openMnemonicScreen(accountName: String?, addAccountPayload: AddAccountPayload) {
        val payload = BackupMnemonicPayload.Create(accountName, addAccountPayload)

        navigationBuilder()
            .addCase(R.id.welcomeFragment, R.id.action_welcomeFragment_to_mnemonic_nav_graph)
            .addCase(R.id.startCreateWalletFragment, R.id.action_startCreateWalletFragment_to_mnemonic_nav_graph)
            .addCase(R.id.walletDetailsFragment, R.id.action_accountDetailsFragment_to_mnemonic_nav_graph)
            .setArgs(BackupMnemonicFragment.getBundle(payload))
            .perform()
    }

    override fun openContribute(payload: ContributePayload) {
        val bundle = CrowdloanContributeFragment.getBundle(payload)

        navigationBuilder()
            .addCase(R.id.mainFragment, R.id.action_mainFragment_to_crowdloanContributeFragment)
            .addCase(R.id.moonbeamCrowdloanTermsFragment, R.id.action_moonbeamCrowdloanTermsFragment_to_crowdloanContributeFragment)
            .setArgs(bundle)
            .perform()
    }

    override val customBonusFlow: Flow<BonusPayload?>
        get() = mainNavController!!.currentBackStackEntry!!.savedStateHandle
            .getLiveData<BonusPayload?>(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)
            .asFlow()

    override val latestCustomBonus: BonusPayload?
        get() = mainNavController!!.currentBackStackEntry!!.savedStateHandle
            .get(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)

    override fun openCustomContribute(payload: CustomContributePayload) {
        navigationBuilder(R.id.action_crowdloanContributeFragment_to_customContributeFragment)
            .setArgs(CustomContributeFragment.getBundle(payload))
            .perform()
    }

    override fun setCustomBonus(payload: BonusPayload) {
        mainNavController!!.previousBackStackEntry!!.savedStateHandle.set(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA, payload)
    }

    override fun openConfirmContribute(payload: ConfirmContributePayload) {
        navigationBuilder(R.id.action_crowdloanContributeFragment_to_confirmContributeFragment)
            .setArgs(ConfirmContributeFragment.getBundle(payload))
            .perform()
    }

    override fun returnToMain() {
        navigationBuilder(R.id.back_to_main)
            .perform()
    }

    override fun openMoonbeamFlow(payload: ContributePayload) {
        navigationBuilder(R.id.action_mainFragment_to_moonbeamCrowdloanTermsFragment)
            .setArgs(MoonbeamCrowdloanTermsFragment.getBundle(payload))
            .perform()
    }

    override fun openAddAccount(payload: AddAccountPayload) {
        navigationBuilder(R.id.action_open_onboarding)
            .setArgs(WelcomeFragment.bundle(payload))
            .perform()
    }

    override fun openFilter(payload: TransactionHistoryFilterPayload) {
        navigationBuilder(R.id.action_mainFragment_to_filterFragment)
            .setArgs(TransactionHistoryFilterFragment.getBundle(payload))
            .perform()
    }

    override fun openSend(payload: SendPayload, initialRecipientAddress: String?) {
        val extras = SelectSendFragment.getBundle(payload, initialRecipientAddress)

        navigationBuilder(R.id.action_open_send)
            .setArgs(extras)
            .perform()
    }

    override fun openConfirmTransfer(transferDraft: TransferDraft) {
        val bundle = ConfirmSendFragment.getBundle(transferDraft)

        navigationBuilder(R.id.action_chooseAmountFragment_to_confirmTransferFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openTransferDetail(transaction: OperationParcelizeModel.Transfer) {
        val bundle = TransferDetailFragment.getBundle(transaction)

        navigationBuilder(R.id.open_transfer_detail)
            .setArgs(bundle)
            .perform()
    }

    override fun openRewardDetail(reward: OperationParcelizeModel.Reward) {
        val bundle = RewardDetailFragment.getBundle(reward)

        navigationBuilder(R.id.open_reward_detail)
            .setArgs(bundle)
            .perform()
    }

    override fun openPoolRewardDetail(reward: OperationParcelizeModel.PoolReward) {
        val bundle = PoolRewardDetailFragment.getBundle(reward)

        navigationBuilder(R.id.open_pool_reward_detail)
            .setArgs(bundle)
            .perform()
    }

    override fun openSwapDetail(swap: OperationParcelizeModel.Swap) {
        val bundle = SwapDetailFragment.getBundle(swap)

        navigationBuilder(R.id.open_swap_detail)
            .setArgs(bundle)
            .perform()
    }

    override fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic) {
        navigationBuilder(R.id.open_extrinsic_detail)
            .setArgs(ExtrinsicDetailFragment.getBundle(extrinsic))
            .perform()
    }

    override fun openWallets() {
        navigationBuilder(R.id.action_open_accounts)
            .perform()
    }

    override fun openSwitchWallet() {
        navigationBuilder(R.id.action_open_switch_wallet)
            .perform()
    }

    override fun openDelegatedAccountsUpdates() {
        navigationBuilder(R.id.action_switchWalletFragment_to_delegatedAccountUpdates)
            .perform()
    }

    override fun openSelectAddress(arguments: Bundle) {
        navigationBuilder(R.id.action_open_select_address)
            .setArgs(arguments)
            .perform()
    }

    override fun openSelectMultipleWallets(arguments: Bundle) {
        navigationBuilder(R.id.action_open_select_multiple_wallets)
            .setArgs(arguments)
            .perform()
    }

    override fun openNodes() {
        navigationBuilder(R.id.action_mainFragment_to_nodesFragment)
            .perform()
    }

    override fun openReceive(assetPayload: AssetPayload) {
        navigationBuilder(R.id.action_open_receive)
            .setArgs(ReceiveFragment.getBundle(assetPayload))
            .perform()
    }

    override fun openAssetSearch() {
        navigationBuilder(R.id.action_mainFragment_to_assetSearchFragment)
            .perform()
    }

    override fun openManageTokens() {
        navigationBuilder(R.id.action_mainFragment_to_manageTokensGraph)
            .perform()
    }

    override fun openManageChainTokens(payload: ManageChainTokensPayload) {
        val args = ManageChainTokensFragment.getBundle(payload)
        navigationBuilder(R.id.action_manageTokensFragment_to_manageChainTokensFragment)
            .setArgs(args)
            .perform()
    }

    override fun openAddTokenSelectChain() {
        navigationBuilder(R.id.action_manageTokensFragment_to_addTokenSelectChainFragment)
            .perform()
    }

    override fun openSendFlow() {
        navigationBuilder(R.id.action_mainFragment_to_sendFlow)
            .perform()
    }

    override fun openReceiveFlow() {
        navigationBuilder(R.id.action_mainFragment_to_receiveFlow)
            .perform()
    }

    override fun openBuyFlow() {
        navigationBuilder(R.id.action_mainFragment_to_buyFlow)
            .perform()
    }

    override fun openBuyFlowFromSendFlow() {
        navigationBuilder(R.id.action_sendFlow_to_buyFlow)
            .perform()
    }

    override fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload) {
        val args = AddTokenEnterInfoFragment.getBundle(payload)
        navigationBuilder(R.id.action_addTokenSelectChainFragment_to_addTokenEnterInfoFragment)
            .setArgs(args)
            .perform()
    }

    override fun finishAddTokenFlow() {
        navigationBuilder(R.id.finish_add_token_flow)
            .perform()
    }

    override fun openWalletConnectSessions(metaId: Long) {
        walletConnectDelegate.openWalletConnectSessions(WalletConnectSessionsPayload(metaId = metaId))
    }

    override fun openWalletConnectScan() {
        walletConnectDelegate.openScanPairingQrCode()
    }

    override fun openStaking() {
        if (mainNavController?.currentDestination?.id != R.id.mainFragment) mainNavController?.navigate(R.id.action_open_split_screen)

        stakingDashboardDelegate.openStakingDashboard()
    }

    override fun closeSendFlow() {
        navigationBuilder(R.id.action_close_send_flow)
            .perform()
    }

    override fun openSendNetworks(payload: NetworkFlowPayload) {
        navigationBuilder(R.id.action_sendFlow_to_sendFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .perform()
    }

    override fun openReceiveNetworks(payload: NetworkFlowPayload) {
        navigationBuilder(R.id.action_receiveFlow_to_receiveFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .perform()
    }

    override fun openSwapNetworks(payload: NetworkSwapFlowPayload) {
        navigationBuilder(R.id.action_selectAssetSwapFlowFragment_to_swapFlowNetworkFragment)
            .setArgs(NetworkSwapFlowFragment.createPayload(payload))
            .perform()
    }

    override fun openBuyNetworks(payload: NetworkFlowPayload) {
        navigationBuilder(R.id.action_buyFlow_to_buyFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .perform()
    }

    override fun returnToMainSwapScreen() {
        navigationBuilder(R.id.action_return_to_swap_settings)
            .perform()
    }

    override fun openSwapFlow() {
        val payload = SwapFlowPayload.InitialSelecting
        navigationBuilder(R.id.action_mainFragment_to_swapFlow)
            .setArgs(AssetSwapFlowFragment.getBundle(payload))
            .perform()
    }

    override fun openSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload) {
        navigationBuilder(R.id.action_open_swapSetupAmount)
            .setArgs(SwapMainSettingsFragment.getBundle(swapSettingsPayload))
            .perform()
    }

    override fun openNfts() {
        navigationBuilder(R.id.action_mainFragment_to_nfts_nav_graph)
            .perform()
    }

    override fun nonCancellableVerify() {
        val currentDestination = mainNavController?.currentDestination

        if (currentDestination?.id == R.id.splashFragment) {
            return
        }

        val action = PinCodeAction.CheckAfterInactivity(BackDelayedNavigation, ToolbarConfiguration())
        val bundle = PincodeFragment.getPinCodeBundle(action)

        if (currentDestination?.id == R.id.pincodeFragment) {
            val currentBackStackEntry = mainNavController!!.currentBackStackEntry
            val arguments = currentBackStackEntry!!.arguments!!.getParcelableCompat<PinCodeAction>(PincodeFragment.KEY_PINCODE_ACTION)
            if (arguments is PinCodeAction.Change) {
                mainNavController?.navigate(R.id.action_pin_code_access_recovery, bundle)
            }
        } else {
            mainNavController?.navigate(R.id.action_pin_code_access_recovery, bundle)
        }
    }

    override fun openUpdateNotifications() {
        navigationBuilder(R.id.action_open_update_notifications)
            .perform()
    }

    override fun openPushWelcome() {
        navigationBuilder(R.id.action_open_pushNotificationsWelcome)
            .perform()
    }

    override fun openCloudBackupSettings() {
        navigationBuilder(R.id.action_open_cloudBackupSettings)
            .perform()
    }

    override fun returnToWallet() {
        // to achieve smooth animation
        postToUiThread {
            navigationBuilder(R.id.action_return_to_wallet)
                .perform()
        }
    }

    override fun openWalletDetails(metaId: Long) {
        val extras = WalletDetailsFragment.getBundle(metaId)
        navigationBuilder(R.id.action_open_account_details)
            .setArgs(extras)
            .perform()
    }

    override fun openNodeDetails(nodeId: Int) {
        val extras = NodeDetailsFragment.getBundle(nodeId)
        navigationBuilder(R.id.action_nodesFragment_to_nodeDetailsFragment)
            .setArgs(extras)
            .perform()
    }

    override fun openAssetDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        navigationBuilder()
            .addCase(R.id.mainFragment, R.id.action_mainFragment_to_balanceDetailFragment)
            .addCase(R.id.assetSearchFragment, R.id.action_assetSearchFragment_to_balanceDetailFragment)
            .addCase(R.id.confirmTransferFragment, R.id.action_confirmTransferFragment_to_balanceDetailFragment)
            .setFallbackCase(R.id.action_root_to_balanceDetailFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openAddNode() {
        navigationBuilder(R.id.action_nodesFragment_to_addNodeFragment)
            .perform()
    }

    override fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount) {
        val bundle = ChangeWatchAccountFragment.getBundle(payload)

        navigationBuilder(R.id.action_accountDetailsFragment_to_changeWatchAccountFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openCreateWallet(payload: StartCreateWalletPayload) {
        navigationBuilder(R.id.action_open_create_new_wallet)
            .setArgs(StartCreateWalletFragment.bundle(payload))
            .perform()
    }

    override fun openUserContributions() {
        navigationBuilder(R.id.action_mainFragment_to_userContributionsFragment)
            .perform()
    }

    override fun getExportMnemonicDelayedNavigation(exportPayload: ExportPayload.ChainAccount): DelayedNavigation {
        val payload = BackupMnemonicPayload.Confirm(exportPayload.chainId, exportPayload.metaId)
        val extras = BackupMnemonicFragment.getBundle(payload)

        return NavComponentDelayedNavigation(R.id.action_open_mnemonic_nav_graph, extras)
    }

    override fun getExportSeedDelayedNavigation(exportPayload: ExportPayload.ChainAccount): DelayedNavigation {
        val extras = ExportSeedFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_seed, extras)
    }

    override fun getExportJsonDelayedNavigation(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportJsonFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_json, extras)
    }

    override fun exportJsonAction(exportPayload: ExportPayload) {
        val extras = ExportJsonFragment.getBundle(exportPayload)

        navigationBuilder(R.id.action_export_json)
            .setArgs(extras)
            .perform()
    }

    override fun finishExportFlow() {
        navigationBuilder(R.id.finish_export_flow)
            .perform()
    }

    override fun openScanImportParitySigner(payload: ParitySignerStartPayload) {
        val args = ScanImportParitySignerFragment.getBundle(payload)

        navigationBuilder(R.id.action_startImportParitySignerFragment_to_scanImportParitySignerFragment)
            .setArgs(args)
            .perform()
    }

    override fun openPreviewImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = PreviewImportParitySignerFragment.getBundle(payload)

        navigationBuilder(R.id.action_scanImportParitySignerFragment_to_previewImportParitySignerFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openFinishImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = FinishImportParitySignerFragment.getBundle(payload)

        navigationBuilder(R.id.action_previewImportParitySignerFragment_to_finishImportParitySignerFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openScanParitySignerSignature(payload: ScanSignParitySignerPayload) {
        val bundle = ScanSignParitySignerFragment.getBundle(payload)

        navigationBuilder(R.id.action_showSignParitySignerFragment_to_scanSignParitySignerFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun finishParitySignerFlow() {
        navigationBuilder(R.id.action_finish_parity_signer_flow)
            .perform()
    }

    override fun openAddLedgerChainAccountFlow(payload: AddAccountPayload.ChainAccount) {
        val bundle = AddChainAccountSelectLedgerFragment.getBundle(payload)

        navigationBuilder(R.id.action_accountDetailsFragment_to_addLedgerAccountGraph)
            .setArgs(bundle)
            .perform()
    }

    override fun openCreateCloudBackupPassword(walletName: String) {
        val bundle = CreateWalletBackupPasswordFragment.getBundle(CreateBackupPasswordPayload(walletName))

        navigationBuilder(R.id.action_startCreateWalletFragment_to_createCloudBackupPasswordFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun restoreCloudBackup() {
        navigationBuilder()
            .addCase(R.id.importWalletOptionsFragment, R.id.action_importWalletOptionsFragment_to_restoreCloudBackup)
            .addCase(R.id.startCreateWalletFragment, R.id.action_startCreateWalletFragment_to_resotreCloudBackupFragment)
            .perform()
    }

    override fun openSyncWalletsBackupPassword() {
        navigationBuilder(R.id.action_cloudBackupSettings_to_syncWalletsBackupPasswordFragment)
            .perform()
    }

    override fun openChangeBackupPasswordFlow() {
        navigationBuilder(R.id.action_cloudBackupSettings_to_checkCloudBackupPasswordFragment)
            .perform()
    }

    override fun openRestoreBackupPassword() {
        navigationBuilder(R.id.action_cloudBackupSettings_to_restoreCloudBackupPasswordFragment)
            .perform()
    }

    override fun openChangeBackupPassword() {
        navigationBuilder(R.id.action_checkCloudBackupPasswordFragment_to_changeBackupPasswordFragment)
            .perform()
    }

    override fun openManualBackupSelectAccount(metaId: Long) {
        val bundle = ManualBackupSelectAccountFragment.bundle(ManualBackupSelectAccountPayload(metaId))

        navigationBuilder(R.id.action_manualBackupSelectWalletFragment_to_manualBackupSelectAccountFragment)
            .setArgs(bundle)
            .perform()
    }

    override fun openManualBackupConditions(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupWarningFragment.bundle(payload)

        val pinCodePayload = PinCodeAction.Check(
            NavComponentDelayedNavigation(R.id.action_manualBackupPincodeFragment_to_manualBackupWarning, bundle),
            ToolbarConfiguration()
        )
        val pinCodeBundle = PincodeFragment.getPinCodeBundle(pinCodePayload)

        navigationBuilder()
            .addCase(R.id.manualBackupSelectWallet, R.id.action_manualBackupSelectWallet_to_pincode_check)
            .addCase(R.id.manualBackupSelectAccount, R.id.action_manualBackupSelectAccount_to_pincode_check)
            .setArgs(pinCodeBundle)
            .perform()
    }

    override fun openManualBackupSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupSecretsFragment.bundle(payload)
        navigationBuilder(R.id.action_manualBackupWarning_to_manualBackupSecrets)
            .setArgs(bundle)
            .perform()
    }

    override fun openManualBackupAdvancedSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupAdvancedSecretsFragment.bundle(payload)
        navigationBuilder(R.id.action_manualBackupSecrets_to_manualBackupAdvancedSecrets)
            .setArgs(bundle)
            .perform()
    }

    override fun openCreateWatchWallet() {
        navigationBuilder(R.id.action_importWalletOptionsFragment_to_createWatchWalletFragment)
            .perform()
    }

    override fun openStartImportParitySigner() {
        openStartImportPolkadotVault(PolkadotVaultVariant.PARITY_SIGNER)
    }

    override fun openStartImportPolkadotVault() {
        openStartImportPolkadotVault(PolkadotVaultVariant.POLKADOT_VAULT)
    }

    override fun openImportOptionsScreen() {
        navigationBuilder()
            .addCase(R.id.welcomeFragment, R.id.action_welcomeFragment_to_importWalletOptionsFragment)
            .setFallbackCase(R.id.action_importWalletOptionsFragment)
            .perform()
    }

    override fun openStartImportLegacyLedger() {
        navigationBuilder(R.id.action_importWalletOptionsFragment_to_import_legacy_ledger_graph)
            .perform()
    }

    override fun openStartImportGenericLedger() {
        navigationBuilder(R.id.action_importWalletOptionsFragment_to_import_generic_ledger_graph)
            .perform()
    }

    override fun withPinCodeCheckRequired(
        delayedNavigation: DelayedNavigation,
        createMode: Boolean,
        pinCodeTitleRes: Int?,
    ) {
        val action = if (createMode) {
            PinCodeAction.Create(delayedNavigation)
        } else {
            PinCodeAction.Check(delayedNavigation, ToolbarConfiguration(pinCodeTitleRes, true))
        }

        navigationBuilder(R.id.open_pincode_check)
            .setArgs(PincodeFragment.getPinCodeBundle(action))
            .perform()
    }

    private fun openStartImportPolkadotVault(variant: PolkadotVaultVariant) {
        val args = StartImportParitySignerFragment.getBundle(ParitySignerStartPayload(variant))

        navigationBuilder(R.id.action_importWalletOptionsFragment_to_import_parity_signer_graph)
            .setArgs(args)
            .perform()
    }

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_split_screen)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }
}
