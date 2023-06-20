package io.novafoundation.nova.app.root.navigation

import android.os.Bundle
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.utils.getParcelableCompat
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.AccountDetailsFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.ExportJsonPasswordFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedFragment
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountFragment
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
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.ChangeWatchAccountFragment
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment
import io.novafoundation.nova.feature_assets.presentation.send.TransferDraft
import io.novafoundation.nova.feature_assets.presentation.send.amount.SelectSendFragment
import io.novafoundation.nova.feature_assets.presentation.send.confirm.ConfirmSendFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.add.enterInfo.AddTokenEnterInfoPayload
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensFragment
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.chain.ManageChainTokensPayload
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.ExtrinsicDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.RewardDetailFragment
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
import io.novafoundation.nova.splash.SplashRouter
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.Flow

@Parcelize
class NavComponentDelayedNavigation(val globalActionId: Int, val extras: Bundle? = null) : DelayedNavigation

@Parcelize
object BackDelayedNavigation : DelayedNavigation

class Navigator(
    private val navigationHolder: NavigationHolder,
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

    override fun openCreateAccount() {
        navController?.navigate(R.id.action_welcomeFragment_to_createAccountFragment)
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
        }
    }

    override fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload) {
        val bundle = ConfirmMnemonicFragment.getBundle(confirmMnemonicPayload)

        navController?.navigate(
            R.id.action_backupMnemonicFragment_to_confirmMnemonicFragment,
            bundle
        )
    }

    override fun openImportAccountScreen(payload: ImportAccountPayload) {
        val destination = when (val currentDestinationId = navController?.currentDestination?.id) {
            R.id.welcomeFragment -> R.id.action_welcomeFragment_to_import_nav_graph
            R.id.accountDetailsFragment -> R.id.action_accountDetailsFragment_to_import_nav_graph
            else -> throw IllegalArgumentException("Unknown current destination to open import account screen: $currentDestinationId")
        }

        navController?.navigate(destination, ImportAccountFragment.getBundle(payload))
    }

    override fun openMnemonicScreen(accountName: String?, addAccountPayload: AddAccountPayload) {
        val destination = when (val currentDestinationId = navController?.currentDestination?.id) {
            R.id.welcomeFragment -> R.id.action_welcomeFragment_to_mnemonic_nav_graph
            R.id.createAccountFragment -> R.id.action_createAccountFragment_to_mnemonic_nav_graph
            R.id.accountDetailsFragment -> R.id.action_accountDetailsFragment_to_mnemonic_nav_graph
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

    override fun openFilter(payload: TransactionHistoryFilterPayload) = performNavigation(
        actionId = R.id.action_mainFragment_to_filterFragment,
        args = TransactionHistoryFilterFragment.getBundle(payload)
    )

    override fun openSend(assetPayload: AssetPayload, initialRecipientAddress: String?) {
        val extras = SelectSendFragment.getBundle(assetPayload, initialRecipientAddress)

        navController?.navigate(R.id.action_open_send, extras)
    }

    override fun openConfirmTransfer(transferDraft: TransferDraft) {
        val bundle = ConfirmSendFragment.getBundle(transferDraft)

        navController?.navigate(R.id.action_chooseAmountFragment_to_confirmTransferFragment, bundle)
    }

    override fun finishSendFlow() {
        navController?.navigate(R.id.finish_send_flow)
    }

    override fun openTransferDetail(transaction: OperationParcelizeModel.Transfer) {
        val bundle = TransferDetailFragment.getBundle(transaction)

        navController?.navigate(R.id.open_transfer_detail, bundle)
    }

    override fun openRewardDetail(reward: OperationParcelizeModel.Reward) {
        val bundle = RewardDetailFragment.getBundle(reward)

        navController?.navigate(R.id.open_reward_detail, bundle)
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

    override fun openSelectAddress(arguments: Bundle) {
        navController?.navigate(R.id.action_open_select_address, arguments)
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

    override fun openAddTokenEnterInfo(payload: AddTokenEnterInfoPayload) {
        val args = AddTokenEnterInfoFragment.getBundle(payload)
        navController?.navigate(R.id.action_addTokenSelectChainFragment_to_addTokenEnterInfoFragment, args)
    }

    override fun finishAddTokenFlow() {
        navController?.navigate(R.id.finish_add_token_flow)
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

    override fun returnToWallet() {
        // to achieve smooth animation
        postToUiThread {
            navController?.navigate(R.id.action_return_to_wallet)
        }
    }

    override fun openAccountDetails(metaId: Long) {
        val extras = AccountDetailsFragment.getBundle(metaId)

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
            else -> null
        }

        action?.let { navController?.navigate(it, bundle) }
    }

    override fun openAddNode() {
        navController?.navigate(R.id.action_nodesFragment_to_addNodeFragment)
    }

    override fun openChangeWatchAccount(payload: AddAccountPayload.ChainAccount) {
        val bundle = ChangeWatchAccountFragment.getBundle(payload)

        navController?.navigate(R.id.action_accountDetailsFragment_to_changeWatchAccountFragment, bundle)
    }

    override fun openAddAccount(payload: AddAccountPayload) {
        navController?.navigate(R.id.action_open_onboarding, WelcomeFragment.bundle(payload))
    }

    override fun openUserContributions() {
        navController?.navigate(R.id.action_mainFragment_to_userContributionsFragment)
    }

    override fun exportMnemonicAction(exportPayload: ExportPayload): DelayedNavigation {
        val payload = BackupMnemonicPayload.Confirm(exportPayload.chainId, exportPayload.metaId)
        val extras = BackupMnemonicFragment.getBundle(payload)

        return NavComponentDelayedNavigation(R.id.action_open_mnemonic_nav_graph, extras)
    }

    override fun exportSeedAction(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportSeedFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_seed, extras)
    }

    override fun exportJsonPasswordAction(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportJsonPasswordFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_json, extras)
    }

    override fun openExportJsonConfirm(payload: ExportJsonConfirmPayload) {
        val extras = ExportJsonConfirmFragment.getBundle(payload)

        navController?.navigate(R.id.action_exportJsonPasswordFragment_to_exportJsonConfirmFragment, extras)
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

    override fun openCreateWatchWallet() {
        navController?.navigate(R.id.action_welcomeFragment_to_createWatchWalletFragment)
    }

    override fun openStartImportParitySigner() {
        openStartImportPolkadotVault(PolkadotVaultVariant.PARITY_SIGNER)
    }

    override fun openStartImportPolkadotVault() {
        openStartImportPolkadotVault(PolkadotVaultVariant.POLKADOT_VAULT)
    }

    override fun openStartImportLedger() {
        navController?.navigate(R.id.action_welcomeFragment_to_import_ledger_graph)
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
        navController?.navigate(R.id.action_welcomeFragment_to_import_parity_signer_graph, args)
    }

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_main)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }
}
