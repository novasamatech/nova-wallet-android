package io.novafoundation.nova.app.root.navigation

import android.os.Bundle
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.delayedNavigation.BackDelayedNavigation
import io.novafoundation.nova.app.root.navigation.delayedNavigation.NavComponentDelayedNavigation
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
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendFragment
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_assets.presentation.send.confirm.ConfirmSendFragment
import io.novafoundation.nova.feature_assets.presentation.swap.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload
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
    private val navigationHolder: NavigationHolder,
    private val walletConnectDelegate: WalletConnectRouter,
    private val stakingDashboardDelegate: StakingDashboardRouter
) : BaseNavigator(navigationHolder),
    SplashRouter,
    OnboardingRouter,
    AccountRouter,
    AssetsRouter,
    RootRouter,
    CrowdloanRouter {

    private val navController: NavController?
        get() = navigationHolder.navController

    override fun openWelcomeScreen() {
        when (navController?.currentDestination?.id) {
            R.id.accountsFragment -> navController?.navigate(R.id.action_walletManagment_to_welcome, WelcomeFragment.bundle(false))
            R.id.splashFragment -> navController?.navigate(R.id.action_splash_to_onboarding, WelcomeFragment.bundle(false))
        }
    }

    override fun openInitialCheckPincode() {
        val action = PinCodeAction.Check(NavComponentDelayedNavigation(R.id.action_open_main), ToolbarConfiguration())
        val bundle = PincodeFragment.getPinCodeBundle(action)
        navController?.navigate(R.id.action_splash_to_pin, bundle)
    }

    override fun openCreateFirstWallet() {
        navController?.navigate(
            R.id.action_welcomeFragment_to_startCreateWallet,
            StartCreateWalletFragment.bundle(StartCreateWalletPayload(FlowType.FIRST_WALLET))
        )
    }

    override fun openMain() {
        navController?.navigate(R.id.action_open_main)
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

                navController?.navigate(delayedNavigation.globalActionId, delayedNavigation.extras, navOptions)
            }

            is BackDelayedNavigation -> {
                navController?.popBackStack()
            }
        }
    }

    override fun openCreatePincode() {
        val bundle = buildCreatePinBundle()

        when (navController?.currentDestination?.id) {
            R.id.splashFragment -> navController?.navigate(R.id.action_splash_to_pin, bundle)
            R.id.importAccountFragment -> navController?.navigate(R.id.action_importAccountFragment_to_pincodeFragment, bundle)
            R.id.confirmMnemonicFragment -> navController?.navigate(R.id.action_confirmMnemonicFragment_to_pincodeFragment, bundle)
            R.id.createWatchWalletFragment -> navController?.navigate(R.id.action_watchWalletFragment_to_pincodeFragment, bundle)
            R.id.finishImportParitySignerFragment -> navController?.navigate(R.id.action_finishImportParitySignerFragment_to_pincodeFragment, bundle)
            R.id.finishImportLedgerFragment -> navController?.navigate(R.id.action_finishImportLedgerFragment_to_pincodeFragment, bundle)
            R.id.createCloudBackupPasswordFragment -> navController?.navigate(R.id.action_createCloudBackupPasswordFragment_to_pincodeFragment, bundle)
            R.id.restoreCloudBackupFragment -> navController?.navigate(R.id.action_restoreCloudBackupFragment_to_pincodeFragment, bundle)
            R.id.finishImportGenericLedgerFragment -> navController?.navigate(R.id.action_finishImportGenericLedgerFragment_to_pincodeFragment, bundle)
        }
    }

    override fun openAdvancedSettings(payload: AdvancedEncryptionModePayload) {
        navController?.navigate(R.id.action_open_advancedEncryptionFragment, AdvancedEncryptionFragment.getBundle(payload))
    }

    override fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload) {
        val bundle = ConfirmMnemonicFragment.getBundle(confirmMnemonicPayload)

        navController?.navigate(
            R.id.action_backupMnemonicFragment_to_confirmMnemonicFragment,
            bundle
        )
    }

    override fun openImportAccountScreen(payload: ImportAccountPayload) {
        val currentDestination = navController?.currentDestination ?: return
        val actionId = when (currentDestination.id) {
            // Wee need the splash fragment case to close app if we use back navigation in import mnemonic screen
            R.id.splashFragment -> R.id.action_splashFragment_to_import_nav_graph
            else -> R.id.action_import_nav_graph
        }
        navController?.navigate(actionId, ImportAccountFragment.getBundle(payload))
    }

    override fun openMnemonicScreen(accountName: String?, addAccountPayload: AddAccountPayload) {
        val destination = when (val currentDestinationId = navController?.currentDestination?.id) {
            R.id.welcomeFragment -> R.id.action_welcomeFragment_to_mnemonic_nav_graph
            R.id.startCreateWalletFragment -> R.id.action_startCreateWalletFragment_to_mnemonic_nav_graph
            R.id.walletDetailsFragment -> R.id.action_accountDetailsFragment_to_mnemonic_nav_graph
            else -> throw IllegalArgumentException("Unknown current destination to open mnemonic screen: $currentDestinationId")
        }

        val payload = BackupMnemonicPayload.Create(accountName, addAccountPayload)
        navController?.navigate(destination, BackupMnemonicFragment.getBundle(payload))
    }

    override fun openContribute(payload: ContributePayload) {
        val bundle = CrowdloanContributeFragment.getBundle(payload)

        when (navController?.currentDestination?.id) {
            R.id.mainFragment -> navController?.navigate(R.id.action_mainFragment_to_crowdloanContributeFragment, bundle)
            R.id.moonbeamCrowdloanTermsFragment -> navController?.navigate(R.id.action_moonbeamCrowdloanTermsFragment_to_crowdloanContributeFragment, bundle)
        }
    }

    override val customBonusFlow: Flow<BonusPayload?>
        get() = navController!!.currentBackStackEntry!!.savedStateHandle
            .getLiveData<BonusPayload?>(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)
            .asFlow()

    override val latestCustomBonus: BonusPayload?
        get() = navController!!.currentBackStackEntry!!.savedStateHandle
            .get(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)

    override fun openCustomContribute(payload: CustomContributePayload) {
        navController?.navigate(R.id.action_crowdloanContributeFragment_to_customContributeFragment, CustomContributeFragment.getBundle(payload))
    }

    override fun setCustomBonus(payload: BonusPayload) {
        navController!!.previousBackStackEntry!!.savedStateHandle.set(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA, payload)
    }

    override fun openConfirmContribute(payload: ConfirmContributePayload) {
        navController?.navigate(R.id.action_crowdloanContributeFragment_to_confirmContributeFragment, ConfirmContributeFragment.getBundle(payload))
    }

    override fun back() {
        navigationHolder.executeBack()
    }

    override fun returnToMain() {
        navController?.navigate(R.id.back_to_main)
    }

    override fun openMoonbeamFlow(payload: ContributePayload) {
        navController?.navigate(R.id.action_mainFragment_to_moonbeamCrowdloanTermsFragment, MoonbeamCrowdloanTermsFragment.getBundle(payload))
    }

    override fun openAddAccount(payload: AddAccountPayload) {
        navController?.navigate(R.id.action_open_onboarding, WelcomeFragment.bundle(payload))
    }

    override fun openFilter(payload: TransactionHistoryFilterPayload) = performNavigation(
        actionId = R.id.action_mainFragment_to_filterFragment,
        args = TransactionHistoryFilterFragment.getBundle(payload)
    )

    override fun openSend(payload: SendPayload, initialRecipientAddress: String?) {
        val extras = SelectSendFragment.getBundle(payload, initialRecipientAddress)

        navController?.navigate(R.id.action_open_send, extras)
    }

    override fun openConfirmTransfer(transferDraft: TransferDraft) {
        val bundle = ConfirmSendFragment.getBundle(transferDraft)

        navController?.navigate(R.id.action_chooseAmountFragment_to_confirmTransferFragment, bundle)
    }

    override fun openTransferDetail(transaction: OperationParcelizeModel.Transfer) {
        val bundle = TransferDetailFragment.getBundle(transaction)

        navController?.navigate(R.id.open_transfer_detail, bundle)
    }

    override fun openRewardDetail(reward: OperationParcelizeModel.Reward) {
        val bundle = RewardDetailFragment.getBundle(reward)

        navController?.navigate(R.id.open_reward_detail, bundle)
    }

    override fun openPoolRewardDetail(reward: OperationParcelizeModel.PoolReward) {
        val bundle = PoolRewardDetailFragment.getBundle(reward)

        navController?.navigate(R.id.open_pool_reward_detail, bundle)
    }

    override fun openSwapDetail(swap: OperationParcelizeModel.Swap) {
        val bundle = SwapDetailFragment.getBundle(swap)

        navController?.navigate(R.id.open_swap_detail, bundle)
    }

    override fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic) {
        val bundle = ExtrinsicDetailFragment.getBundle(extrinsic)

        navController?.navigate(R.id.open_extrinsic_detail, bundle)
    }

    override fun openWallets() {
        navController?.navigate(R.id.action_open_accounts)
    }

    override fun openSwitchWallet() {
        navController?.navigate(R.id.action_open_switch_wallet)
    }

    override fun openDelegatedAccountsUpdates() {
        navController?.navigate(R.id.action_switchWalletFragment_to_delegatedAccountUpdates)
    }

    override fun openSelectAddress(arguments: Bundle) {
        navController?.navigate(R.id.action_open_select_address, arguments)
    }

    override fun openSelectMultipleWallets(arguments: Bundle) {
        navController?.navigate(R.id.action_open_select_multiple_wallets, arguments)
    }

    override fun openNodes() {
        navController?.navigate(R.id.action_mainFragment_to_nodesFragment)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        navController?.navigate(R.id.action_open_receive, ReceiveFragment.getBundle(assetPayload))
    }

    override fun openAssetFilters() {
        navController?.navigate(R.id.action_mainFragment_to_assetFiltersFragment)
    }

    override fun openAssetSearch() {
        navController?.navigate(R.id.action_mainFragment_to_assetSearchFragment)
    }

    override fun openManageTokens() {
        navController?.navigate(R.id.action_mainFragment_to_manageTokensGraph)
    }

    override fun openManageChainTokens(payload: ManageChainTokensPayload) {
        val args = ManageChainTokensFragment.getBundle(payload)
        navController?.navigate(R.id.action_manageTokensFragment_to_manageChainTokensFragment, args)
    }

    override fun openAddTokenSelectChain() {
        navController?.navigate(R.id.action_manageTokensFragment_to_addTokenSelectChainFragment)
    }

    override fun openSendFlow() {
        navController?.navigate(R.id.action_mainFragment_to_sendFlow)
    }

    override fun openReceiveFlow() {
        navController?.navigate(R.id.action_mainFragment_to_receiveFlow)
    }

    override fun openBuyFlow() {
        navController?.navigate(R.id.action_mainFragment_to_buyFlow)
    }

    override fun openBuyFlowFromSendFlow() {
        navController?.navigate(R.id.action_sendFlow_to_buyFlow)
    }

    override fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload) {
        val args = AddTokenEnterInfoFragment.getBundle(payload)
        navController?.navigate(R.id.action_addTokenSelectChainFragment_to_addTokenEnterInfoFragment, args)
    }

    override fun finishAddTokenFlow() {
        navController?.navigate(R.id.finish_add_token_flow)
    }

    override fun openWalletConnectSessions(metaId: Long) {
        walletConnectDelegate.openWalletConnectSessions(WalletConnectSessionsPayload(metaId = metaId))
    }

    override fun openWalletConnectScan() {
        walletConnectDelegate.openScanPairingQrCode()
    }

    override fun openStaking() {
        if (navController?.currentDestination?.id != R.id.mainFragment) navController?.navigate(R.id.action_open_main)

        stakingDashboardDelegate.openStakingDashboard()
    }

    override fun closeSendFlow() {
        navController?.navigate(R.id.action_close_send_flow)
    }

    override fun openSwapFlow() {
        val payload = SwapFlowPayload.InitialSelecting
        navController?.navigate(R.id.action_mainFragment_to_swapFlow, AssetSwapFlowFragment.getBundle(payload))
    }

    override fun openSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload) {
        navController?.navigate(R.id.action_open_swapSetupAmount, SwapMainSettingsFragment.getBundle(swapSettingsPayload))
    }

    override fun openNfts() {
        navController?.navigate(R.id.action_mainFragment_to_nfts_nav_graph)
    }

    override fun nonCancellableVerify() {
        val currentDestination = navController?.currentDestination

        if (currentDestination?.id == R.id.splashFragment) {
            return
        }

        val action = PinCodeAction.CheckAfterInactivity(BackDelayedNavigation, ToolbarConfiguration())
        val bundle = PincodeFragment.getPinCodeBundle(action)

        if (currentDestination?.id == R.id.pincodeFragment) {
            val currentBackStackEntry = navController!!.currentBackStackEntry
            val arguments = currentBackStackEntry!!.arguments!!.getParcelableCompat<PinCodeAction>(PincodeFragment.KEY_PINCODE_ACTION)
            if (arguments is PinCodeAction.Change) {
                navController?.navigate(R.id.action_pin_code_access_recovery, bundle)
            }
        } else {
            navController?.navigate(R.id.action_pin_code_access_recovery, bundle)
        }
    }

    override fun openUpdateNotifications() {
        navController?.navigate(R.id.action_open_update_notifications)
    }

    override fun openPushWelcome() {
        performNavigation(R.id.action_open_pushNotificationsWelcome)
    }

    override fun openCloudBackupSettings() {
        performNavigation(R.id.action_open_cloudBackupSettings)
    }

    override fun returnToWallet() {
        // to achieve smooth animation
        postToUiThread {
            navController?.navigate(R.id.action_return_to_wallet)
        }
    }

    override fun openWalletDetails(metaId: Long) {
        val extras = WalletDetailsFragment.getBundle(metaId)

        navController?.navigate(R.id.action_open_account_details, extras)
    }

    override fun openNodeDetails(nodeId: Int) {
        navController?.navigate(R.id.action_nodesFragment_to_nodeDetailsFragment, NodeDetailsFragment.getBundle(nodeId))
    }

    override fun openAssetDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        val action = when (navController?.currentDestination?.id) {
            R.id.mainFragment -> R.id.action_mainFragment_to_balanceDetailFragment
            R.id.assetSearchFragment -> R.id.action_assetSearchFragment_to_balanceDetailFragment
            R.id.confirmTransferFragment -> R.id.action_confirmTransferFragment_to_balanceDetailFragment
            else -> R.id.action_root_to_balanceDetailFragment
        }

        navController?.navigate(action, bundle)
    }

    override fun openAddNode() {
        navController?.navigate(R.id.action_nodesFragment_to_addNodeFragment)
    }

    override fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount) {
        val bundle = ChangeWatchAccountFragment.getBundle(payload)

        navController?.navigate(R.id.action_accountDetailsFragment_to_changeWatchAccountFragment, bundle)
    }

    override fun openCreateWallet(payload: StartCreateWalletPayload) {
        navController?.navigate(R.id.action_open_create_new_wallet, StartCreateWalletFragment.bundle(payload))
    }

    override fun openUserContributions() {
        navController?.navigate(R.id.action_mainFragment_to_userContributionsFragment)
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

        navController?.navigate(R.id.action_export_json, extras)
    }

    override fun finishExportFlow() {
        navController?.navigate(R.id.finish_export_flow)
    }

    override fun openScanImportParitySigner(payload: ParitySignerStartPayload) {
        val args = ScanImportParitySignerFragment.getBundle(payload)
        navController?.navigate(R.id.action_startImportParitySignerFragment_to_scanImportParitySignerFragment, args)
    }

    override fun openPreviewImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = PreviewImportParitySignerFragment.getBundle(payload)

        navController?.navigate(R.id.action_scanImportParitySignerFragment_to_previewImportParitySignerFragment, bundle)
    }

    override fun openFinishImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = FinishImportParitySignerFragment.getBundle(payload)

        navController?.navigate(R.id.action_previewImportParitySignerFragment_to_finishImportParitySignerFragment, bundle)
    }

    override fun openScanParitySignerSignature(payload: ScanSignParitySignerPayload) {
        val bundle = ScanSignParitySignerFragment.getBundle(payload)

        navController?.navigate(R.id.action_showSignParitySignerFragment_to_scanSignParitySignerFragment, bundle)
    }

    override fun finishParitySignerFlow() {
        navController?.navigate(R.id.action_finish_parity_signer_flow)
    }

    override fun openAddLedgerChainAccountFlow(payload: AddAccountPayload.ChainAccount) {
        val bundle = AddChainAccountSelectLedgerFragment.getBundle(payload)

        navController?.navigate(R.id.action_accountDetailsFragment_to_addLedgerAccountGraph, bundle)
    }

    override fun finishApp() {
        navigationHolder.finishApp()
    }

    override fun openCreateCloudBackupPassword(walletName: String) {
        val bundle = CreateWalletBackupPasswordFragment.getBundle(CreateBackupPasswordPayload(walletName))

        navController?.navigate(R.id.action_startCreateWalletFragment_to_createCloudBackupPasswordFragment, bundle)
    }

    override fun restoreCloudBackup() {
        when (navController?.currentDestination?.id) {
            R.id.importWalletOptionsFragment -> navController?.navigate(R.id.action_importWalletOptionsFragment_to_restoreCloudBackup)
            R.id.startCreateWalletFragment -> navController?.navigate(R.id.action_startCreateWalletFragment_to_resotreCloudBackupFragment)
        }
    }

    override fun openSyncWalletsBackupPassword() {
        performNavigation(R.id.action_cloudBackupSettings_to_syncWalletsBackupPasswordFragment)
    }

    override fun openChangeBackupPasswordFlow() {
        performNavigation(R.id.action_cloudBackupSettings_to_checkCloudBackupPasswordFragment)
    }

    override fun openRestoreBackupPassword() {
        performNavigation(R.id.action_cloudBackupSettings_to_restoreCloudBackupPasswordFragment)
    }

    override fun openChangeBackupPassword() {
        performNavigation(R.id.action_checkCloudBackupPasswordFragment_to_changeBackupPasswordFragment)
    }

    override fun openManualBackupSelectAccount(metaId: Long) {
        val bundle = ManualBackupSelectAccountFragment.bundle(ManualBackupSelectAccountPayload(metaId))
        performNavigation(R.id.action_manualBackupSelectWalletFragment_to_manualBackupSelectAccountFragment, bundle)
    }

    override fun openManualBackupConditions(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupWarningFragment.bundle(payload)

        val pinCodePayload = PinCodeAction.Check(
            NavComponentDelayedNavigation(R.id.action_manualBackupPincodeFragment_to_manualBackupWarning, bundle),
            ToolbarConfiguration()
        )
        val pinCodeBundle = PincodeFragment.getPinCodeBundle(pinCodePayload)

        performNavigation(
            cases = arrayOf(
                R.id.manualBackupSelectWallet to R.id.action_manualBackupSelectWallet_to_pincode_check,
                R.id.manualBackupSelectAccount to R.id.action_manualBackupSelectAccount_to_pincode_check
            ),
            args = pinCodeBundle
        )
    }

    override fun openManualBackupSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupSecretsFragment.bundle(payload)
        performNavigation(R.id.action_manualBackupWarning_to_manualBackupSecrets, bundle)
    }

    override fun openManualBackupAdvancedSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupAdvancedSecretsFragment.bundle(payload)
        performNavigation(R.id.action_manualBackupSecrets_to_manualBackupAdvancedSecrets, bundle)
    }

    override fun openCreateWatchWallet() {
        navController?.navigate(R.id.action_importWalletOptionsFragment_to_createWatchWalletFragment)
    }

    override fun openStartImportParitySigner() {
        openStartImportPolkadotVault(PolkadotVaultVariant.PARITY_SIGNER)
    }

    override fun openStartImportPolkadotVault() {
        openStartImportPolkadotVault(PolkadotVaultVariant.POLKADOT_VAULT)
    }

    override fun openImportOptionsScreen() {
        when (navController?.currentDestination?.id) {
            R.id.welcomeFragment -> navController?.navigate(R.id.action_welcomeFragment_to_importWalletOptionsFragment)
            else -> navController?.navigate(R.id.action_importWalletOptionsFragment)
        }
    }

    override fun openStartImportLegacyLedger() {
        navController?.navigate(R.id.action_importWalletOptionsFragment_to_import_legacy_ledger_graph)
    }

    override fun openStartImportGenericLedger() {
        navController?.navigate(R.id.action_importWalletOptionsFragment_to_import_generic_ledger_graph)
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

        val extras = PincodeFragment.getPinCodeBundle(action)

        navController?.navigate(R.id.open_pincode_check, extras)
    }

    private fun openStartImportPolkadotVault(variant: PolkadotVaultVariant) {
        val args = StartImportParitySignerFragment.getBundle(ParitySignerStartPayload(variant))
        navController?.navigate(R.id.action_importWalletOptionsFragment_to_import_parity_signer_graph, args)
    }

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_main)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }
}
