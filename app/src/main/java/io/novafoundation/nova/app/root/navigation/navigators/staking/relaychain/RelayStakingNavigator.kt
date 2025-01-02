package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
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
) : BaseNavigator(navigationHoldersRegistry), StakingRouter {

    override fun returnToStakingMain() {
        navigationBuilder(R.id.back_to_staking_main)
            .perform()
    }

    override fun openSwitchWallet() = commonNavigator.openSwitchWallet()

    override fun openWalletDetails(metaAccountId: Long) = commonNavigator.openWalletDetails(metaAccountId)

    override fun openCustomRebond() {
        navigationBuilder(R.id.action_stakingFragment_to_customRebondFragment)
            .perform()
    }

    override fun openCurrentValidators() {
        navigationBuilder(R.id.action_stakingFragment_to_currentValidatorsFragment)
            .perform()
    }

    override fun returnToCurrentValidators() {
        navigationBuilder(R.id.action_confirmStakingFragment_back_to_currentValidatorsFragment)
            .perform()
    }

    override fun openChangeRewardDestination() {
        navigationBuilder(R.id.action_stakingFragment_to_selectRewardDestinationFragment)
            .perform()
    }

    override fun openConfirmRewardDestination(payload: ConfirmRewardDestinationPayload) {
        navigationBuilder(R.id.action_selectRewardDestinationFragment_to_confirmRewardDestinationFragment)
            .setArgs(ConfirmRewardDestinationFragment.getBundle(payload))
            .perform()
    }

    override fun openControllerAccount() {
        navigationBuilder(R.id.action_stakingBalanceFragment_to_setControllerAccountFragment)
            .perform()
    }

    override fun openConfirmSetController(payload: ConfirmSetControllerPayload) {
        navigationBuilder(R.id.action_stakingSetControllerAccountFragment_to_confirmSetControllerAccountFragment)
            .setArgs(ConfirmSetControllerFragment.getBundle(payload))
            .perform()
    }

    override fun openRecommendedValidators() {
        navigationBuilder(R.id.action_startChangeValidatorsFragment_to_recommendedValidatorsFragment)
            .perform()
    }

    override fun openSelectCustomValidators() {
        val flowType = when (currentDestination?.id) {
            R.id.setupStakingType -> FlowType.SETUP_STAKING_VALIDATORS
            else -> FlowType.CHANGE_STAKING_VALIDATORS
        }
        val payload = CustomValidatorsPayload(flowType)

        navigationBuilder()
            .addCase(R.id.setupStakingType, R.id.action_setupStakingType_to_selectCustomValidatorsFragment)
            .addCase(R.id.startChangeValidatorsFragment, R.id.action_startChangeValidatorsFragment_to_selectCustomValidatorsFragment)
            .setArgs(SelectCustomValidatorsFragment.getBundle(payload))
            .perform()
    }

    override fun openCustomValidatorsSettings() {
        navigationBuilder(R.id.action_selectCustomValidatorsFragment_to_settingsCustomValidatorsFragment)
            .perform()
    }

    override fun openSearchCustomValidators() {
        navigationBuilder(R.id.action_selectCustomValidatorsFragment_to_searchCustomValidatorsFragment)
            .perform()
    }

    override fun openReviewCustomValidators(payload: CustomValidatorsPayload) {
        navigationBuilder(R.id.action_selectCustomValidatorsFragment_to_reviewCustomValidatorsFragment)
            .setArgs(ReviewCustomValidatorsFragment.getBundle(payload))
            .perform()
    }

    override fun openConfirmStaking() {
        navigationBuilder(R.id.openConfirmStakingFragment)
            .perform()
    }

    override fun openConfirmNominations() {
        navigationBuilder(R.id.action_confirmStakingFragment_to_confirmNominationsFragment)
            .perform()
    }

    override fun openChainStakingMain() {
        navigationBuilder(R.id.action_mainFragment_to_stakingGraph)
            .perform()
    }

    override fun openStartChangeValidators() {
        navigationBuilder(R.id.openStartChangeValidatorsFragment)
            .perform()
    }

    override fun openStory(story: StakingStoryModel) {
        navigationBuilder(R.id.open_staking_story)
            .setArgs(StoryFragment.getBundle(story))
            .perform()
    }

    override fun openPayouts() {
        navigationBuilder(R.id.action_stakingFragment_to_payoutsListFragment)
            .perform()
    }

    override fun openPayoutDetails(payout: PendingPayoutParcelable) {
        navigationBuilder(R.id.action_payoutsListFragment_to_payoutDetailsFragment)
            .setArgs(PayoutDetailsFragment.getBundle(payout))
            .perform()
    }

    override fun openConfirmPayout(payload: ConfirmPayoutPayload) {
        navigationBuilder(R.id.action_open_confirm_payout)
            .setArgs(ConfirmPayoutFragment.getBundle(payload))
            .perform()
    }

    override fun openBondMore() {
        navigationBuilder(R.id.action_open_selectBondMoreFragment)
            .setArgs(SelectBondMoreFragment.getBundle(SelectBondMorePayload()))
            .perform()
    }

    override fun openConfirmBondMore(payload: ConfirmBondMorePayload) {
        navigationBuilder(R.id.action_selectBondMoreFragment_to_confirmBondMoreFragment)
            .setArgs(ConfirmBondMoreFragment.getBundle(payload))
            .perform()
    }

    override fun openSelectUnbond() {
        navigationBuilder(R.id.action_stakingFragment_to_selectUnbondFragment)
            .perform()
    }

    override fun openConfirmUnbond(payload: ConfirmUnbondPayload) {
        navigationBuilder(R.id.action_selectUnbondFragment_to_confirmUnbondFragment)
            .setArgs(ConfirmUnbondFragment.getBundle(payload))
            .perform()
    }

    override fun openRedeem() {
        navigationBuilder(R.id.action_open_redeemFragment)
            .setArgs(RedeemFragment.getBundle(RedeemPayload()))
            .perform()
    }

    override fun openConfirmRebond(payload: ConfirmRebondPayload) {
        navigationBuilder(R.id.action_open_confirm_rebond)
            .setArgs(ConfirmRebondFragment.getBundle(payload))
            .perform()
    }

    override fun openValidatorDetails(payload: StakeTargetDetailsPayload) {
        navigationBuilder(R.id.open_validator_details)
            .setArgs(ValidatorDetailsFragment.getBundle(payload))
            .perform()
    }

    override fun openRebag() {
        navigationBuilder(R.id.action_stakingFragment_to_rebag)
            .perform()
    }

    override fun openStakingPeriods() {
        navigationBuilder(R.id.action_stakingFragment_to_staking_periods)
            .perform()
    }

    override fun openSetupStakingType() {
        navigationBuilder(R.id.action_setupAmountMultiStakingFragment_to_setupStakingType)
            .perform()
    }

    override fun openSelectPool(payload: SelectingPoolPayload) {
        val arguments = SelectPoolFragment.getBundle(payload)
        navigationBuilder(R.id.action_setupStakingType_to_selectCustomPoolFragment)
            .setArgs(arguments)
            .perform()
    }

    override fun openSearchPool(payload: SelectingPoolPayload) {
        val arguments = SearchPoolFragment.getBundle(payload)
        navigationBuilder(R.id.action_selectPool_to_searchPoolFragment)
            .setArgs(arguments)
            .perform()
    }

    override fun finishSetupValidatorsFlow() {
        navigationBuilder(R.id.action_back_to_setupAmountMultiStakingFragment)
            .perform()
    }

    override fun finishSetupPoolFlow() {
        navigationBuilder()
            .addCase(R.id.searchPoolFragment, R.id.action_searchPool_to_setupAmountMultiStakingFragment)
            .addCase(R.id.selectPoolFragment, R.id.action_selectPool_to_setupAmountMultiStakingFragment)
            .perform()
    }

    override fun finishRedeemFlow(redeemConsequences: RedeemConsequences) {
        if (redeemConsequences.willKillStash) {
            stakingDashboardRouter.returnToStakingDashboard()
        } else {
            returnToStakingMain()
        }
    }

    override fun openAddStakingProxy() {
        navigationBuilder(R.id.action_open_addStakingProxyFragment)
            .perform()
    }

    override fun openConfirmAddStakingProxy(payload: ConfirmAddStakingProxyPayload) {
        navigationBuilder(R.id.action_addStakingProxyFragment_to_confirmAddStakingProxyFragment)
            .setArgs(ConfirmAddStakingProxyFragment.getBundle(payload))
            .perform()
    }

    override fun openStakingProxyList() {
        navigationBuilder(R.id.action_open_stakingProxyList)
            .perform()
    }

    override fun openConfirmRemoveStakingProxy(payload: ConfirmRemoveStakingProxyPayload) {
        navigationBuilder(R.id.action_open_confirmRemoveStakingProxyFragment)
            .setArgs(ConfirmRemoveStakingProxyFragment.getBundle(payload))
            .perform()
    }

    override fun openDAppBrowser(url: String) {
        navigationBuilder(R.id.action_open_dappBrowser)
            .setArgs(DAppBrowserFragment.getBundle(DAppBrowserPayload.Address(url)))
            .perform()
    }
}
