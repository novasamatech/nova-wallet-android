package io.novafoundation.nova.app.root.navigation.navigators

import android.os.Bundle
import androidx.lifecycle.asFlow
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.delayedNavigation.BackDelayedNavigation
import io.novafoundation.nova.app.root.navigation.delayedNavigation.NavComponentDelayedNavigation
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
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
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorFragment
import io.novafoundation.nova.feature_account_impl.presentation.legacyAddress.ChainAddressSelectorPayload
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
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardFragment
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardPayload
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
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val walletConnectDelegate: WalletConnectRouter,
    private val stakingDashboardDelegate: StakingDashboardRouter
) : BaseNavigator(navigationHoldersRegistry),
    SplashRouter,
    OnboardingRouter,
    AccountRouter,
    AssetsRouter,
    RootRouter,
    CrowdloanRouter,
    DelayedNavigationRouter {

    override fun openWelcomeScreen() {
        navigationBuilder().cases()
            .addCase(R.id.accountsFragment, R.id.action_walletManagment_to_welcome)
            .addCase(R.id.splashFragment, R.id.action_splash_to_onboarding)
            .setArgs(WelcomeFragment.bundle(false))
            .navigateInFirstAttachedContext()
    }

    override fun openInitialCheckPincode() {
        val action = PinCodeAction.Check(NavComponentDelayedNavigation(R.id.action_open_split_screen), ToolbarConfiguration())

        navigationBuilder().action(R.id.action_splash_to_pin)
            .setArgs(PincodeFragment.getPinCodeBundle(action))
            .navigateInRoot()
    }

    override fun openCreateFirstWallet() {
        navigationBuilder().action(R.id.action_welcomeFragment_to_startCreateWallet)
            .setArgs(StartCreateWalletFragment.bundle(StartCreateWalletPayload(FlowType.FIRST_WALLET)))
            .navigateInFirstAttachedContext()
    }

    override fun openMain() {
        navigationBuilder().action(R.id.action_open_split_screen)
            .navigateInRoot()
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

                navigationBuilder().action(delayedNavigation.globalActionId)
                    .setArgs(delayedNavigation.extras)
                    .setNavOptions(navOptions)
                    .navigateInFirstAttachedContext()
            }

            is BackDelayedNavigation -> back()
        }
    }

    override fun openCreatePincode() {
        val args = buildCreatePinBundle()

        navigationBuilder().cases()
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
            .navigateInRoot()
    }

    override fun openAdvancedSettings(payload: AdvancedEncryptionModePayload) {
        navigationBuilder().action(R.id.action_open_advancedEncryptionFragment)
            .setArgs(AdvancedEncryptionFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload) {
        navigationBuilder().action(R.id.action_backupMnemonicFragment_to_confirmMnemonicFragment)
            .setArgs(ConfirmMnemonicFragment.getBundle(confirmMnemonicPayload))
            .navigateInFirstAttachedContext()
    }

    override fun openImportAccountScreen(payload: ImportAccountPayload) {
        navigationBuilder().cases()
            .addCase(R.id.splashFragment, R.id.action_splashFragment_to_import_nav_graph)
            .setFallbackCase(R.id.action_import_nav_graph)
            .setArgs(ImportAccountFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openMnemonicScreen(accountName: String?, addAccountPayload: AddAccountPayload) {
        val payload = BackupMnemonicPayload.Create(accountName, addAccountPayload)

        navigationBuilder().cases()
            .addCase(R.id.welcomeFragment, R.id.action_welcomeFragment_to_mnemonic_nav_graph)
            .addCase(R.id.startCreateWalletFragment, R.id.action_startCreateWalletFragment_to_mnemonic_nav_graph)
            .addCase(R.id.walletDetailsFragment, R.id.action_accountDetailsFragment_to_mnemonic_nav_graph)
            .setArgs(BackupMnemonicFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openContribute(payload: ContributePayload) {
        val bundle = CrowdloanContributeFragment.getBundle(payload)

        navigationBuilder().cases()
            .addCase(R.id.mainFragment, R.id.action_mainFragment_to_crowdloanContributeFragment)
            .addCase(R.id.moonbeamCrowdloanTermsFragment, R.id.action_moonbeamCrowdloanTermsFragment_to_crowdloanContributeFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    @Deprecated("TODO: Use communicator api instead")
    override val customBonusFlow: Flow<BonusPayload?>
        get() = currentBackStackEntry!!.savedStateHandle
            .getLiveData<BonusPayload?>(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)
            .asFlow()

    @Deprecated("TODO: Use communicator api instead")
    override val latestCustomBonus: BonusPayload?
        get() = currentBackStackEntry!!.savedStateHandle
            .get(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA)

    override fun openCustomContribute(payload: CustomContributePayload) {
        navigationBuilder().action(R.id.action_crowdloanContributeFragment_to_customContributeFragment)
            .setArgs(CustomContributeFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    @Deprecated("TODO: Use communicator api instead")
    override fun setCustomBonus(payload: BonusPayload) {
        previousBackStackEntry!!.savedStateHandle.set(CrowdloanContributeFragment.KEY_BONUS_LIVE_DATA, payload)
    }

    override fun openConfirmContribute(payload: ConfirmContributePayload) {
        navigationBuilder().action(R.id.action_crowdloanContributeFragment_to_confirmContributeFragment)
            .setArgs(ConfirmContributeFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun returnToMain() {
        navigationBuilder().action(R.id.back_to_main)
            .navigateInFirstAttachedContext()
    }

    override fun openMoonbeamFlow(payload: ContributePayload) {
        navigationBuilder().action(R.id.action_mainFragment_to_moonbeamCrowdloanTermsFragment)
            .setArgs(MoonbeamCrowdloanTermsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openAddAccount(payload: AddAccountPayload) {
        navigationBuilder().action(R.id.action_open_onboarding)
            .setArgs(WelcomeFragment.bundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openFilter(payload: TransactionHistoryFilterPayload) {
        navigationBuilder().action(R.id.action_mainFragment_to_filterFragment)
            .setArgs(TransactionHistoryFilterFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSend(payload: SendPayload, initialRecipientAddress: String?) {
        val extras = SelectSendFragment.getBundle(payload, initialRecipientAddress)

        navigationBuilder().action(R.id.action_open_send)
            .setArgs(extras)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmTransfer(transferDraft: TransferDraft) {
        val bundle = ConfirmSendFragment.getBundle(transferDraft)

        navigationBuilder().action(R.id.action_chooseAmountFragment_to_confirmTransferFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openTransferDetail(transaction: OperationParcelizeModel.Transfer) {
        val bundle = TransferDetailFragment.getBundle(transaction)

        navigationBuilder().action(R.id.open_transfer_detail)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openRewardDetail(reward: OperationParcelizeModel.Reward) {
        val bundle = RewardDetailFragment.getBundle(reward)

        navigationBuilder().action(R.id.open_reward_detail)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openPoolRewardDetail(reward: OperationParcelizeModel.PoolReward) {
        val bundle = PoolRewardDetailFragment.getBundle(reward)

        navigationBuilder().action(R.id.open_pool_reward_detail)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapDetail(swap: OperationParcelizeModel.Swap) {
        val bundle = SwapDetailFragment.getBundle(swap)

        navigationBuilder().action(R.id.open_swap_detail)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openExtrinsicDetail(extrinsic: OperationParcelizeModel.Extrinsic) {
        navigationBuilder().action(R.id.open_extrinsic_detail)
            .setArgs(ExtrinsicDetailFragment.getBundle(extrinsic))
            .navigateInFirstAttachedContext()
    }

    override fun openWallets() {
        navigationBuilder().action(R.id.action_open_accounts)
            .navigateInFirstAttachedContext()
    }

    override fun openSwitchWallet() {
        navigationBuilder().action(R.id.action_open_switch_wallet)
            .navigateInFirstAttachedContext()
    }

    override fun openDelegatedAccountsUpdates() {
        navigationBuilder().action(R.id.action_switchWalletFragment_to_delegatedAccountUpdates)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectAddress(arguments: Bundle) {
        navigationBuilder().action(R.id.action_open_select_address)
            .setArgs(arguments)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectMultipleWallets(arguments: Bundle) {
        navigationBuilder().action(R.id.action_open_select_multiple_wallets)
            .setArgs(arguments)
            .navigateInFirstAttachedContext()
    }

    override fun openNodes() {
        navigationBuilder().action(R.id.action_mainFragment_to_nodesFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openReceive(assetPayload: AssetPayload) {
        navigationBuilder().action(R.id.action_open_receive)
            .setArgs(ReceiveFragment.getBundle(assetPayload))
            .navigateInFirstAttachedContext()
    }

    override fun openAssetSearch() {
        navigationBuilder().action(R.id.action_mainFragment_to_assetSearchFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openManageTokens() {
        navigationBuilder().action(R.id.action_mainFragment_to_manageTokensGraph)
            .navigateInFirstAttachedContext()
    }

    override fun openManageChainTokens(payload: ManageChainTokensPayload) {
        val args = ManageChainTokensFragment.getBundle(payload)
        navigationBuilder().action(R.id.action_manageTokensFragment_to_manageChainTokensFragment)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    override fun openAddTokenSelectChain() {
        navigationBuilder().action(R.id.action_manageTokensFragment_to_addTokenSelectChainFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSendFlow() {
        navigationBuilder().action(R.id.action_mainFragment_to_sendFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openReceiveFlow() {
        navigationBuilder().action(R.id.action_mainFragment_to_receiveFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openBuyFlow() {
        navigationBuilder().action(R.id.action_mainFragment_to_buyFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openBuyFlowFromSendFlow() {
        navigationBuilder().action(R.id.action_sendFlow_to_buyFlow)
            .navigateInFirstAttachedContext()
    }

    override fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload) {
        val args = AddTokenEnterInfoFragment.getBundle(payload)
        navigationBuilder().action(R.id.action_addTokenSelectChainFragment_to_addTokenEnterInfoFragment)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    override fun finishAddTokenFlow() {
        navigationBuilder().action(R.id.finish_add_token_flow)
            .navigateInFirstAttachedContext()
    }

    override fun openWalletConnectSessions(metaId: Long) {
        walletConnectDelegate.openWalletConnectSessions(WalletConnectSessionsPayload(metaId = metaId))
    }

    override fun openWalletConnectScan() {
        walletConnectDelegate.openScanPairingQrCode()
    }

    override fun openStaking() {
        if (currentDestination?.id != R.id.mainFragment) {
            navigationBuilder().action(R.id.action_open_split_screen)
                .navigateInFirstAttachedContext()
        }

        stakingDashboardDelegate.openStakingDashboard()
    }

    override fun closeSendFlow() {
        navigationBuilder().action(R.id.action_close_send_flow)
            .navigateInFirstAttachedContext()
    }

    override fun openNovaCard() {
        navigationBuilder().action(R.id.action_open_novaCard)
            .navigateInFirstAttachedContext()
    }

    override fun finishAndAwaitTopUp() {
        navigationBuilder().action(R.id.action_finish_top_up_flow)
            .navigateInFirstAttachedContext()
    }

    override fun openAwaitingCardCreation() {
        navigationBuilder().action(R.id.action_open_awaiting_card_creation)
            .navigateInFirstAttachedContext()
    }

    override fun openSendNetworks(payload: NetworkFlowPayload) {
        navigationBuilder().action(R.id.action_sendFlow_to_sendFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openReceiveNetworks(payload: NetworkFlowPayload) {
        navigationBuilder().action(R.id.action_receiveFlow_to_receiveFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSwapNetworks(payload: NetworkSwapFlowPayload) {
        navigationBuilder().action(R.id.action_selectAssetSwapFlowFragment_to_swapFlowNetworkFragment)
            .setArgs(NetworkSwapFlowFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openBuyNetworks(payload: NetworkFlowPayload) {
        navigationBuilder().action(R.id.action_buyFlow_to_buyFlowNetwork)
            .setArgs(NetworkFlowFragment.createPayload(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openChainAddressSelector(chainId: String, accountId: ByteArray) {
        val payload = ChainAddressSelectorPayload(chainId, accountId)

        navigationBuilder().action(R.id.action_openUnifiedAddressDialog)
            .setArgs(ChainAddressSelectorFragment.getBundle(payload))
            .navigateInRoot()
    }

    override fun closeChainAddressesSelector() {
        navigationBuilder().action(R.id.action_closeChainAddressesFragment)
            .navigateInRoot()
    }

    override fun returnToMainSwapScreen() {
        navigationBuilder().action(R.id.action_return_to_swap_settings)
            .navigateInFirstAttachedContext()
    }

    override fun openSwapFlow() {
        val payload = SwapFlowPayload.InitialSelecting
        navigationBuilder().action(R.id.action_mainFragment_to_swapFlow)
            .setArgs(AssetSwapFlowFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload) {
        navigationBuilder().action(R.id.action_open_swapSetupAmount)
            .setArgs(SwapMainSettingsFragment.getBundle(swapSettingsPayload))
            .navigateInFirstAttachedContext()
    }

    override fun openTopUpCard(payload: TopUpCardPayload) {
        navigationBuilder().action(R.id.action_open_topUpCard)
            .setArgs(TopUpCardFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun closeTopUp() {
        navigationBuilder().action(R.id.action_close_top_up_with_browser)
            .navigateInFirstAttachedContext()
    }

    override fun finishSelectAndOpenSwapSetupAmount(swapSettingsPayload: SwapSettingsPayload) {
        navigationBuilder().action(R.id.action_finish_and_open_swap_settings)
            .setArgs(SwapMainSettingsFragment.getBundle(swapSettingsPayload))
            .navigateInFirstAttachedContext()
    }

    override fun openNfts() {
        navigationBuilder().action(R.id.action_mainFragment_to_nfts_nav_graph)
            .navigateInFirstAttachedContext()
    }

    override fun nonCancellableVerify() {
        if (currentDestination?.id == R.id.splashFragment) {
            return
        }

        val action = PinCodeAction.CheckAfterInactivity(BackDelayedNavigation, ToolbarConfiguration())
        val bundle = PincodeFragment.getPinCodeBundle(action)

        if (currentDestination?.id == R.id.pincodeFragment) {
            val arguments = currentBackStackEntry!!.arguments!!.getParcelableCompat<PinCodeAction>(PincodeFragment.KEY_PINCODE_ACTION)
            if (arguments is PinCodeAction.Change) {
                navigationBuilder().action(R.id.action_pin_code_access_recovery)
                    .setArgs(bundle)
                    .navigateInRoot()
            }
        } else {
            navigationBuilder().action(R.id.action_pin_code_access_recovery)
                .setArgs(bundle)
                .navigateInRoot()
        }
    }

    override fun openUpdateNotifications() {
        navigationBuilder().action(R.id.action_open_update_notifications)
            .navigateInRoot()
    }

    override fun openPushWelcome() {
        navigationBuilder().action(R.id.action_open_pushNotificationsWelcome)
            .navigateInFirstAttachedContext()
    }

    override fun openCloudBackupSettings() {
        navigationBuilder().action(R.id.action_open_cloudBackupSettings)
            .navigateInFirstAttachedContext()
    }

    override fun returnToWallet() {
        // to achieve smooth animation
        postToUiThread {
            navigationBuilder().action(R.id.action_return_to_wallet)
                .navigateInFirstAttachedContext()
        }
    }

    override fun openWalletDetails(metaId: Long) {
        val extras = WalletDetailsFragment.getBundle(metaId)
        navigationBuilder().action(R.id.action_open_account_details)
            .setArgs(extras)
            .navigateInFirstAttachedContext()
    }

    override fun openNodeDetails(nodeId: Int) {
        val extras = NodeDetailsFragment.getBundle(nodeId)
        navigationBuilder().action(R.id.action_nodesFragment_to_nodeDetailsFragment)
            .setArgs(extras)
            .navigateInFirstAttachedContext()
    }

    override fun openAssetDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        navigationBuilder().cases()
            .addCase(R.id.mainFragment, R.id.action_mainFragment_to_balanceDetailFragment)
            .addCase(R.id.assetSearchFragment, R.id.action_assetSearchFragment_to_balanceDetailFragment)
            .addCase(R.id.confirmTransferFragment, R.id.action_confirmTransferFragment_to_balanceDetailFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openAddNode() {
        navigationBuilder().action(R.id.action_nodesFragment_to_addNodeFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount) {
        val bundle = ChangeWatchAccountFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_accountDetailsFragment_to_changeWatchAccountFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openCreateWallet(payload: StartCreateWalletPayload) {
        navigationBuilder().action(R.id.action_open_create_new_wallet)
            .setArgs(StartCreateWalletFragment.bundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openUserContributions() {
        navigationBuilder().action(R.id.action_mainFragment_to_userContributionsFragment)
            .navigateInFirstAttachedContext()
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

        navigationBuilder().action(R.id.action_export_json)
            .setArgs(extras)
            .navigateInFirstAttachedContext()
    }

    override fun finishExportFlow() {
        navigationBuilder().action(R.id.finish_export_flow)
            .navigateInFirstAttachedContext()
    }

    override fun openScanImportParitySigner(payload: ParitySignerStartPayload) {
        val args = ScanImportParitySignerFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_startImportParitySignerFragment_to_scanImportParitySignerFragment)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    override fun openPreviewImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = PreviewImportParitySignerFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_scanImportParitySignerFragment_to_previewImportParitySignerFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openFinishImportParitySigner(payload: ParitySignerAccountPayload) {
        val bundle = FinishImportParitySignerFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_previewImportParitySignerFragment_to_finishImportParitySignerFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openScanParitySignerSignature(payload: ScanSignParitySignerPayload) {
        val bundle = ScanSignParitySignerFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_showSignParitySignerFragment_to_scanSignParitySignerFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun finishParitySignerFlow() {
        navigationBuilder().action(R.id.action_finish_parity_signer_flow)
            .navigateInFirstAttachedContext()
    }

    override fun openAddLedgerChainAccountFlow(payload: AddAccountPayload.ChainAccount) {
        val bundle = AddChainAccountSelectLedgerFragment.getBundle(payload)

        navigationBuilder().action(R.id.action_accountDetailsFragment_to_addLedgerAccountGraph)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openCreateCloudBackupPassword(walletName: String) {
        val bundle = CreateWalletBackupPasswordFragment.getBundle(CreateBackupPasswordPayload(walletName))

        navigationBuilder().action(R.id.action_startCreateWalletFragment_to_createCloudBackupPasswordFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun restoreCloudBackup() {
        navigationBuilder().cases()
            .addCase(R.id.importWalletOptionsFragment, R.id.action_importWalletOptionsFragment_to_restoreCloudBackup)
            .addCase(R.id.startCreateWalletFragment, R.id.action_startCreateWalletFragment_to_resotreCloudBackupFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSyncWalletsBackupPassword() {
        navigationBuilder().action(R.id.action_cloudBackupSettings_to_syncWalletsBackupPasswordFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChangeBackupPasswordFlow() {
        navigationBuilder().action(R.id.action_cloudBackupSettings_to_checkCloudBackupPasswordFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openRestoreBackupPassword() {
        navigationBuilder().action(R.id.action_cloudBackupSettings_to_restoreCloudBackupPasswordFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChangeBackupPassword() {
        navigationBuilder().action(R.id.action_checkCloudBackupPasswordFragment_to_changeBackupPasswordFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openManualBackupSelectAccount(metaId: Long) {
        val bundle = ManualBackupSelectAccountFragment.bundle(ManualBackupSelectAccountPayload(metaId))

        navigationBuilder().action(R.id.action_manualBackupSelectWalletFragment_to_manualBackupSelectAccountFragment)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openManualBackupConditions(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupWarningFragment.bundle(payload)

        val pinCodePayload = PinCodeAction.Check(
            NavComponentDelayedNavigation(R.id.action_manualBackupPincodeFragment_to_manualBackupWarning, bundle),
            ToolbarConfiguration()
        )
        val pinCodeBundle = PincodeFragment.getPinCodeBundle(pinCodePayload)

        navigationBuilder().cases()
            .addCase(R.id.manualBackupSelectWallet, R.id.action_manualBackupSelectWallet_to_pincode_check)
            .addCase(R.id.manualBackupSelectAccount, R.id.action_manualBackupSelectAccount_to_pincode_check)
            .setArgs(pinCodeBundle)
            .navigateInFirstAttachedContext()
    }

    override fun openManualBackupSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupSecretsFragment.bundle(payload)
        navigationBuilder().action(R.id.action_manualBackupWarning_to_manualBackupSecrets)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openManualBackupAdvancedSecrets(payload: ManualBackupCommonPayload) {
        val bundle = ManualBackupAdvancedSecretsFragment.bundle(payload)
        navigationBuilder().action(R.id.action_manualBackupSecrets_to_manualBackupAdvancedSecrets)
            .setArgs(bundle)
            .navigateInFirstAttachedContext()
    }

    override fun openCreateWatchWallet() {
        navigationBuilder().action(R.id.action_importWalletOptionsFragment_to_createWatchWalletFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openStartImportParitySigner() {
        openStartImportPolkadotVault(PolkadotVaultVariant.PARITY_SIGNER)
    }

    override fun openStartImportPolkadotVault() {
        openStartImportPolkadotVault(PolkadotVaultVariant.POLKADOT_VAULT)
    }

    override fun openImportOptionsScreen() {
        navigationBuilder().cases()
            .addCase(R.id.welcomeFragment, R.id.action_welcomeFragment_to_importWalletOptionsFragment)
            .setFallbackCase(R.id.action_importWalletOptionsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openStartImportLegacyLedger() {
        navigationBuilder().action(R.id.action_importWalletOptionsFragment_to_import_legacy_ledger_graph)
            .navigateInFirstAttachedContext()
    }

    override fun openStartImportGenericLedger() {
        navigationBuilder().action(R.id.action_importWalletOptionsFragment_to_import_generic_ledger_graph)
            .navigateInFirstAttachedContext()
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

        navigationBuilder().action(R.id.open_pincode_check)
            .setArgs(PincodeFragment.getPinCodeBundle(action))
            .navigateInFirstAttachedContext()
    }

    private fun openStartImportPolkadotVault(variant: PolkadotVaultVariant) {
        val args = StartImportParitySignerFragment.getBundle(ParitySignerStartPayload(variant))

        navigationBuilder().action(R.id.action_importWalletOptionsFragment_to_import_parity_signer_graph)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_split_screen)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }

    override fun runDelayedNavigation(delayedNavigation: DelayedNavigation) {
        when (delayedNavigation) {
            BackDelayedNavigation -> back()
            is NavComponentDelayedNavigation -> {
                navigationBuilder().action(delayedNavigation.globalActionId)
                    .setArgs(delayedNavigation.extras)
                    .navigateInFirstAttachedContext()
            }
        }
    }
}
