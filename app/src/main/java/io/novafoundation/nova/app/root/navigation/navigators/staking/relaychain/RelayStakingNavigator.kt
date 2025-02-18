package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.ConfirmPayoutFragment
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.detail.PayoutDetailsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool.SearchPoolFragment
import io.novafoundation.nova.feature_staking_impl.presentation.pools.selectPool.SelectPoolFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMoreFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyPayload
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
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload.FlowType
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewCustomValidatorsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.SelectCustomValidatorsFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment

class RelayStakingNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry,
    private val commonNavigator: Navigator,
    private val stakingDashboardRouter: StakingDashboardRouter,
    private val dAppRouter: DAppRouter
) : BaseNavigator(navigationHoldersRegistry), StakingRouter {

    override fun returnToStakingMain() {
        navigationBuilder().action(R.id.back_to_staking_main)
            .navigateInFirstAttachedContext()
    }

    override fun openSwitchWallet() = commonNavigator.openSwitchWallet()

    override fun openWalletDetails(metaAccountId: Long) = commonNavigator.openWalletDetails(metaAccountId)

    override fun openCustomRebond() {
        navigationBuilder().action(R.id.action_stakingFragment_to_customRebondFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openCurrentValidators() {
        navigationBuilder().action(R.id.action_stakingFragment_to_currentValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun returnToCurrentValidators() {
        navigationBuilder().action(R.id.action_confirmStakingFragment_back_to_currentValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChangeRewardDestination() {
        navigationBuilder().action(R.id.action_stakingFragment_to_selectRewardDestinationFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmRewardDestination(payload: ConfirmRewardDestinationPayload) {
        navigationBuilder().action(R.id.action_selectRewardDestinationFragment_to_confirmRewardDestinationFragment)
            .setArgs(ConfirmRewardDestinationFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openControllerAccount() {
        navigationBuilder().action(R.id.action_stakingBalanceFragment_to_setControllerAccountFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmSetController(payload: ConfirmSetControllerPayload) {
        navigationBuilder().action(R.id.action_stakingSetControllerAccountFragment_to_confirmSetControllerAccountFragment)
            .setArgs(ConfirmSetControllerFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRecommendedValidators() {
        navigationBuilder().action(R.id.action_startChangeValidatorsFragment_to_recommendedValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectCustomValidators() {
        val flowType = when (currentDestination?.id) {
            R.id.setupStakingType -> FlowType.SETUP_STAKING_VALIDATORS
            else -> FlowType.CHANGE_STAKING_VALIDATORS
        }
        val payload = CustomValidatorsPayload(flowType)

        navigationBuilder().cases()
            .addCase(R.id.setupStakingType, R.id.action_setupStakingType_to_selectCustomValidatorsFragment)
            .addCase(R.id.startChangeValidatorsFragment, R.id.action_startChangeValidatorsFragment_to_selectCustomValidatorsFragment)
            .setArgs(SelectCustomValidatorsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openCustomValidatorsSettings() {
        navigationBuilder().action(R.id.action_selectCustomValidatorsFragment_to_settingsCustomValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openSearchCustomValidators() {
        navigationBuilder().action(R.id.action_selectCustomValidatorsFragment_to_searchCustomValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openReviewCustomValidators(payload: CustomValidatorsPayload) {
        navigationBuilder().action(R.id.action_selectCustomValidatorsFragment_to_reviewCustomValidatorsFragment)
            .setArgs(ReviewCustomValidatorsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmStaking() {
        navigationBuilder().action(R.id.openConfirmStakingFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmNominations() {
        navigationBuilder().action(R.id.action_confirmStakingFragment_to_confirmNominationsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openChainStakingMain() {
        navigationBuilder().action(R.id.action_mainFragment_to_stakingGraph)
            .navigateInFirstAttachedContext()
    }

    override fun openStartChangeValidators() {
        navigationBuilder().action(R.id.openStartChangeValidatorsFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openStory(story: StakingStoryModel) {
        navigationBuilder().action(R.id.open_staking_story)
            .setArgs(StoryFragment.getBundle(story))
            .navigateInFirstAttachedContext()
    }

    override fun openPayouts() {
        navigationBuilder().action(R.id.action_stakingFragment_to_payoutsListFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openPayoutDetails(payout: PendingPayoutParcelable) {
        navigationBuilder().action(R.id.action_payoutsListFragment_to_payoutDetailsFragment)
            .setArgs(PayoutDetailsFragment.getBundle(payout))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmPayout(payload: ConfirmPayoutPayload) {
        navigationBuilder().action(R.id.action_open_confirm_payout)
            .setArgs(ConfirmPayoutFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openBondMore() {
        navigationBuilder().action(R.id.action_open_selectBondMoreFragment)
            .setArgs(SelectBondMoreFragment.getBundle(SelectBondMorePayload()))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmBondMore(payload: ConfirmBondMorePayload) {
        navigationBuilder().action(R.id.action_selectBondMoreFragment_to_confirmBondMoreFragment)
            .setArgs(ConfirmBondMoreFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openSelectUnbond() {
        navigationBuilder().action(R.id.action_stakingFragment_to_selectUnbondFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmUnbond(payload: ConfirmUnbondPayload) {
        navigationBuilder().action(R.id.action_selectUnbondFragment_to_confirmUnbondFragment)
            .setArgs(ConfirmUnbondFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRedeem() {
        navigationBuilder().action(R.id.action_open_redeemFragment)
            .setArgs(RedeemFragment.getBundle(RedeemPayload()))
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmRebond(payload: ConfirmRebondPayload) {
        navigationBuilder().action(R.id.action_open_confirm_rebond)
            .setArgs(ConfirmRebondFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openValidatorDetails(payload: StakeTargetDetailsPayload) {
        navigationBuilder().action(R.id.open_validator_details)
            .setArgs(ValidatorDetailsFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openRebag() {
        navigationBuilder().action(R.id.action_stakingFragment_to_rebag)
            .navigateInFirstAttachedContext()
    }

    override fun openStakingPeriods() {
        navigationBuilder().action(R.id.action_stakingFragment_to_staking_periods)
            .navigateInFirstAttachedContext()
    }

    override fun openSetupStakingType() {
        navigationBuilder().action(R.id.action_setupAmountMultiStakingFragment_to_setupStakingType)
            .navigateInFirstAttachedContext()
    }

    override fun openSelectPool(payload: SelectingPoolPayload) {
        val arguments = SelectPoolFragment.getBundle(payload)
        navigationBuilder().action(R.id.action_setupStakingType_to_selectCustomPoolFragment)
            .setArgs(arguments)
            .navigateInFirstAttachedContext()
    }

    override fun openSearchPool(payload: SelectingPoolPayload) {
        val arguments = SearchPoolFragment.getBundle(payload)
        navigationBuilder().action(R.id.action_selectPool_to_searchPoolFragment)
            .setArgs(arguments)
            .navigateInFirstAttachedContext()
    }

    override fun finishSetupValidatorsFlow() {
        navigationBuilder().action(R.id.action_back_to_setupAmountMultiStakingFragment)
            .navigateInFirstAttachedContext()
    }

    override fun finishSetupPoolFlow() {
        navigationBuilder().cases()
            .addCase(R.id.searchPoolFragment, R.id.action_searchPool_to_setupAmountMultiStakingFragment)
            .addCase(R.id.selectPoolFragment, R.id.action_selectPool_to_setupAmountMultiStakingFragment)
            .navigateInFirstAttachedContext()
    }

    override fun finishRedeemFlow(redeemConsequences: RedeemConsequences) {
        if (redeemConsequences.willKillStash) {
            stakingDashboardRouter.returnToStakingDashboard()
        } else {
            returnToStakingMain()
        }
    }

    override fun openAddStakingProxy() {
        navigationBuilder().action(R.id.action_open_addStakingProxyFragment)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmAddStakingProxy(payload: ConfirmAddStakingProxyPayload) {
        navigationBuilder().action(R.id.action_addStakingProxyFragment_to_confirmAddStakingProxyFragment)
            .setArgs(ConfirmAddStakingProxyFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openStakingProxyList() {
        navigationBuilder().action(R.id.action_open_stakingProxyList)
            .navigateInFirstAttachedContext()
    }

    override fun openConfirmRemoveStakingProxy(payload: ConfirmRemoveStakingProxyPayload) {
        navigationBuilder().action(R.id.action_open_confirmRemoveStakingProxyFragment)
            .setArgs(ConfirmRemoveStakingProxyFragment.getBundle(payload))
            .navigateInFirstAttachedContext()
    }

    override fun openDAppBrowser(url: String) {
        dAppRouter.openDAppBrowser(DAppBrowserPayload.Address(url))
    }
}
