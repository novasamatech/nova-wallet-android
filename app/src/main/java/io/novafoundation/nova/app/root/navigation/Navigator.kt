package io.novafoundation.nova.app.root.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.utils.postToUiThread
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.create.CreateAccountFragment
import io.novafoundation.nova.feature_account_impl.presentation.account.details.AccountDetailsFragment
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountListFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password.ExportJsonPasswordFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.mnemonic.ExportMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.seed.ExportSeedFragment
import io.novafoundation.nova.feature_account_impl.presentation.importing.ImportAccountFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.node.details.NodeDetailsFragment
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PinCodeAction
import io.novafoundation.nova.feature_account_impl.presentation.pincode.PincodeFragment
import io.novafoundation.nova.feature_account_impl.presentation.pincode.ToolbarConfiguration
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.ConfirmContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms.MoonbeamCrowdloanTermsFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.CrowdloanContributeFragment
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_onboarding_impl.presentation.welcome.WelcomeFragment
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.ConfirmPayoutFragment
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.PayoutDetailsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm.ConfirmSetControllerFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem.RedeemFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem.RedeemPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.ConfirmRewardDestinationFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.story.StoryFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.ValidatorDetailsParcelModel
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail.BalanceDetailFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_wallet_impl.presentation.receive.ReceiveFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferDraft
import io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.ChooseAmountFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm.ConfirmTransferFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.ChooseRecipientFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.extrinsic.ExtrinsicDetailFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.reward.RewardDetailFragment
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.transfer.TransferDetailFragment
import io.novafoundation.nova.splash.SplashRouter
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.flow.Flow

@Parcelize
class NavComponentDelayedNavigation(val globalActionId: Int, val extras: Bundle? = null) : DelayedNavigation

class Navigator :
    SplashRouter,
    OnboardingRouter,
    AccountRouter,
    WalletRouter,
    RootRouter,
    StakingRouter,
    CrowdloanRouter {

    private var navController: NavController? = null
    private var activity: AppCompatActivity? = null

    fun attach(navController: NavController, activity: AppCompatActivity) {
        this.navController = navController
        this.activity = activity
    }

    fun detach() {
        navController = null
        activity = null
    }

    override fun openAddFirstAccount() {
        navController?.navigate(R.id.action_splash_to_onboarding, WelcomeFragment.bundle(false))
    }

    override fun openInitialCheckPincode() {
        val action = PinCodeAction.Check(NavComponentDelayedNavigation(R.id.action_open_main), ToolbarConfiguration())
        val bundle = PincodeFragment.getPinCodeBundle(action)
        navController?.navigate(R.id.action_splash_to_pin, bundle)
    }

    override fun openCreateAccount(addAccountPayload: AddAccountPayload) {
        navController?.navigate(R.id.action_welcomeFragment_to_createAccountFragment, CreateAccountFragment.getBundle(addAccountPayload))
    }

    override fun openMain() {
        navController?.navigate(R.id.action_open_main)
    }

    override fun openAfterPinCode(delayedNavigation: DelayedNavigation) {
        require(delayedNavigation is NavComponentDelayedNavigation)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.pincodeFragment, true)
            .setEnterAnim(R.anim.fragment_open_enter)
            .setExitAnim(R.anim.fragment_open_exit)
            .setPopEnterAnim(R.anim.fragment_close_enter)
            .setPopExitAnim(R.anim.fragment_close_exit)
            .build()

        navController?.navigate(delayedNavigation.globalActionId, delayedNavigation.extras, navOptions)
    }

    override fun openCreatePincode() {
        val bundle = buildCreatePinBundle()

        when (navController?.currentDestination?.id) {
            R.id.splashFragment -> navController?.navigate(R.id.action_splash_to_pin, bundle)
            R.id.importAccountFragment -> navController?.navigate(R.id.action_importAccountFragment_to_pincodeFragment, bundle)
            R.id.confirmMnemonicFragment -> navController?.navigate(R.id.action_confirmMnemonicFragment_to_pincodeFragment, bundle)
        }
    }

    override fun openConfirmMnemonicOnCreate(confirmMnemonicPayload: ConfirmMnemonicPayload) {
        val bundle = ConfirmMnemonicFragment.getBundle(confirmMnemonicPayload)

        navController?.navigate(
            R.id.action_backupMnemonicFragment_to_confirmMnemonicFragment,
            bundle
        )
    }

    override fun openAboutScreen() {
        navController?.navigate(R.id.action_profileFragment_to_aboutFragment)
    }

    override fun openImportAccountScreen(addAccountPayload: AddAccountPayload) {
        navController?.navigate(R.id.importAction, ImportAccountFragment.getBundle(addAccountPayload))
    }

    override fun openMnemonicScreen(accountName: String, payload: AddAccountPayload) {
        val bundle = BackupMnemonicFragment.getBundle(accountName, payload)
        navController?.navigate(R.id.action_createAccountFragment_to_backupMnemonicFragment, bundle)
    }

    override fun openSetupStaking() {
        navController?.navigate(R.id.action_mainFragment_to_setupStakingFragment)
    }

    override fun openStartChangeValidators() {
        navController?.navigate(R.id.openStartChangeValidatorsFragment)
    }

    override fun openStory(story: StakingStoryModel) {
        navController?.navigate(R.id.open_staking_story, StoryFragment.getBundle(story))
    }

    override fun openPayouts() {
        navController?.navigate(R.id.action_mainFragment_to_payoutsListFragment)
    }

    override fun openPayoutDetails(payout: PendingPayoutParcelable) {
        navController?.navigate(R.id.action_payoutsListFragment_to_payoutDetailsFragment, PayoutDetailsFragment.getBundle(payout))
    }

    override fun openConfirmPayout(payload: ConfirmPayoutPayload) {
        navController?.navigate(R.id.action_open_confirm_payout, ConfirmPayoutFragment.getBundle(payload))
    }

    override fun openStakingBalance() {
        navController?.navigate(R.id.action_mainFragment_to_stakingBalanceFragment)
    }

    override fun openBondMore(payload: SelectBondMorePayload) {
        navController?.navigate(R.id.action_open_selectBondMoreFragment, SelectBondMoreFragment.getBundle(payload))
    }

    override fun openConfirmBondMore(payload: ConfirmBondMorePayload) {
        navController?.navigate(R.id.action_selectBondMoreFragment_to_confirmBondMoreFragment, ConfirmBondMoreFragment.getBundle(payload))
    }

    override fun returnToStakingBalance() {
        navController?.navigate(R.id.action_return_to_staking_balance)
    }

    override fun openSelectUnbond() {
        navController?.navigate(R.id.action_stakingBalanceFragment_to_selectUnbondFragment)
    }

    override fun openConfirmUnbond(payload: ConfirmUnbondPayload) {
        navController?.navigate(R.id.action_selectUnbondFragment_to_confirmUnbondFragment, ConfirmUnbondFragment.getBundle(payload))
    }

    override fun openRedeem(payload: RedeemPayload) {
        navController?.navigate(R.id.action_open_redeemFragment, RedeemFragment.getBundle(payload))
    }

    override fun openConfirmRebond(payload: ConfirmRebondPayload) {
        navController?.navigate(R.id.action_open_confirm_rebond, ConfirmRebondFragment.getBundle(payload))
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
        val popped = navController!!.popBackStack()

        if (!popped) {
            activity!!.finish()
        }
    }

    override fun openCustomRebond() {
        navController?.navigate(R.id.action_stakingBalanceFragment_to_customRebondFragment)
    }

    override fun openCurrentValidators() {
        navController?.navigate(R.id.action_mainFragment_to_currentValidatorsFragment)
    }

    override fun returnToCurrentValidators() {
        navController?.navigate(R.id.action_confirmStakingFragment_back_to_currentValidatorsFragment)
    }

    override fun openChangeRewardDestination() {
        navController?.navigate(R.id.action_mainFragment_to_selectRewardDestinationFragment)
    }

    override fun openConfirmRewardDestination(payload: ConfirmRewardDestinationPayload) {
        navController?.navigate(
            R.id.action_selectRewardDestinationFragment_to_confirmRewardDestinationFragment,
            ConfirmRewardDestinationFragment.getBundle(payload)
        )
    }

    override val currentStackEntryLifecycle: Lifecycle
        get() = navController!!.currentBackStackEntry!!.lifecycle

    override fun openControllerAccount() {
        navController?.navigate(R.id.action_stakingBalanceFragment_to_setControllerAccountFragment)
    }

    override fun openConfirmSetController(payload: ConfirmSetControllerPayload) {
        navController?.navigate(
            R.id.action_stakingSetControllerAccountFragment_to_confirmSetControllerAccountFragment,
            ConfirmSetControllerFragment.getBundle(payload)
        )
    }

    override fun openRecommendedValidators() {
        navController?.navigate(R.id.action_startChangeValidatorsFragment_to_recommendedValidatorsFragment)
    }

    override fun openSelectCustomValidators() {
        navController?.navigate(R.id.action_startChangeValidatorsFragment_to_selectCustomValidatorsFragment)
    }

    override fun openCustomValidatorsSettings() {
        navController?.navigate(R.id.action_selectCustomValidatorsFragment_to_settingsCustomValidatorsFragment)
    }

    override fun openSearchCustomValidators() {
        navController?.navigate(R.id.action_selectCustomValidatorsFragment_to_searchCustomValidatorsFragment)
    }

    override fun openReviewCustomValidators() {
        navController?.navigate(R.id.action_selectCustomValidatorsFragment_to_reviewCustomValidatorsFragment)
    }

    override fun openConfirmStaking() {
        navController?.navigate(R.id.openConfirmStakingFragment)
    }

    override fun openConfirmNominations() {
        navController?.navigate(R.id.action_confirmStakingFragment_to_confirmNominationsFragment)
    }

    override fun returnToMain() {
        navController?.navigate(R.id.back_to_main)
    }

    override fun openMoonbeamFlow(payload: ContributePayload) {
        navController?.navigate(R.id.action_mainFragment_to_moonbeamCrowdloanTermsFragment, MoonbeamCrowdloanTermsFragment.getBundle(payload))
    }

    override fun openValidatorDetails(validatorDetails: ValidatorDetailsParcelModel) {
        navController?.navigate(R.id.open_validator_details, ValidatorDetailsFragment.getBundle(validatorDetails))
    }

    override fun openChooseRecipient(assetPayload: AssetPayload) {
        navController?.navigate(R.id.action_open_send, ChooseRecipientFragment.getBundle(assetPayload))
    }

    override fun openFilter() {
        navController?.navigate(R.id.action_mainFragment_to_filterFragment)
    }

    override fun openChooseAmount(recipientAddress: String, assetPayload: AssetPayload) {
        val bundle = ChooseAmountFragment.getBundle(recipientAddress, assetPayload)

        navController?.navigate(R.id.action_chooseRecipientFragment_to_chooseAmountFragment, bundle)
    }

    override fun openConfirmTransfer(transferDraft: TransferDraft) {
        val bundle = ConfirmTransferFragment.getBundle(transferDraft)

        navController?.navigate(R.id.action_chooseAmountFragment_to_confirmTransferFragment, bundle)
    }

    override fun finishSendFlow() {
        navController?.navigate(R.id.finish_send_flow)
    }

    override fun openRepeatTransaction(recipientAddress: String, assetPayload: AssetPayload) {
        val bundle = ChooseAmountFragment.getBundle(recipientAddress, assetPayload)

        navController?.navigate(R.id.openSelectAmount, bundle)
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

    override fun openAccounts(accountChosenNavDirection: AccountChosenNavDirection) {
        navController?.navigate(R.id.action_open_accounts, AccountListFragment.getBundle(accountChosenNavDirection))
    }

    override fun openNodes() {
        navController?.navigate(R.id.action_mainFragment_to_nodesFragment)
    }

    override fun openLanguages() {
        navController?.navigate(R.id.action_mainFragment_to_languagesFragment)
    }

    override fun openChangeAccountFromWallet() {
        openAccounts(AccountChosenNavDirection.BACK)
    }

    override fun openChangeAccountFromStaking() {
        openAccounts(AccountChosenNavDirection.BACK)
    }

    override fun openReceive(assetPayload: AssetPayload) {
        navController?.navigate(R.id.action_open_receive, ReceiveFragment.getBundle(assetPayload))
    }

    override fun returnToWallet() {
        // to achieve smooth animation
        postToUiThread {
            navController?.navigate(R.id.action_return_to_wallet)
        }
    }

    override fun openAccountDetails(metaAccountId: Long) {
        val extras = AccountDetailsFragment.getBundle(metaAccountId)

        navController?.navigate(R.id.action_open_account_details, extras)
    }

    override fun openEditAccounts() {
        navController?.navigate(R.id.action_accountsFragment_to_editAccountsFragment)
    }

    override fun backToMainScreen() {
        navController?.navigate(R.id.action_editAccountsFragment_to_mainFragment)
    }

    override fun openNodeDetails(nodeId: Int) {
        navController?.navigate(R.id.action_nodesFragment_to_nodeDetailsFragment, NodeDetailsFragment.getBundle(nodeId))
    }

    override fun openAssetDetails(assetPayload: AssetPayload) {
        val bundle = BalanceDetailFragment.getBundle(assetPayload)

        navController?.navigate(R.id.action_mainFragment_to_balanceDetailFragment, bundle)
    }

    override fun openAddNode() {
        navController?.navigate(R.id.action_nodesFragment_to_addNodeFragment)
    }

    override fun openAddAccount(payload: AddAccountPayload) {
        navController?.navigate(R.id.action_open_onboarding, WelcomeFragment.bundle(payload))
    }

    override fun openUserContributions() {
        navController?.navigate(R.id.action_mainFragment_to_userContributionsFragment)
    }

    override fun exportMnemonicAction(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportMnemonicFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_mnemonic, extras)
    }

    override fun exportSeedAction(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportSeedFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_seed, extras)
    }

    override fun exportJsonPasswordAction(exportPayload: ExportPayload): DelayedNavigation {
        val extras = ExportJsonPasswordFragment.getBundle(exportPayload)

        return NavComponentDelayedNavigation(R.id.action_export_json, extras)
    }

    override fun openConfirmMnemonicOnExport(mnemonic: List<String>) {
        val extras = ConfirmMnemonicFragment.getBundle(ConfirmMnemonicPayload(mnemonic, null))

        navController?.navigate(R.id.action_exportMnemonicFragment_to_confirmExportMnemonicFragment, extras)
    }

    override fun openExportJsonConfirm(payload: ExportJsonConfirmPayload) {
        val extras = ExportJsonConfirmFragment.getBundle(payload)

        navController?.navigate(R.id.action_exportJsonPasswordFragment_to_exportJsonConfirmFragment, extras)
    }

    override fun finishExportFlow() {
        navController?.navigate(R.id.finish_export_flow)
    }

    override fun openChangePinCode() {
        val action = PinCodeAction.Change
        val bundle = PincodeFragment.getPinCodeBundle(action)
        navController?.navigate(R.id.action_mainFragment_to_pinCodeFragment, bundle)
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

    private fun buildCreatePinBundle(): Bundle {
        val delayedNavigation = NavComponentDelayedNavigation(R.id.action_open_main)
        val action = PinCodeAction.Create(delayedNavigation)
        return PincodeFragment.getPinCodeBundle(action)
    }
}
