package io.novafoundation.nova.app.root.navigation.staking.relaychain

import androidx.navigation.NavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAppBrowserFragment
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
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.StartStakingLandingFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondFragment
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.story.StoryFragment
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.ValidatorDetailsFragment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RelayStakingNavigator(
    navigationHolder: NavigationHolder,
    private val commonNavigator: Navigator,
) : BaseNavigator(navigationHolder), StakingRouter {

    var stakingTabNavController: NavController? = null

    override fun returnToStakingMain() = performNavigation(R.id.back_to_staking_main)

    override fun openSwitchWallet() = commonNavigator.openSwitchWallet()

    override fun openAccountDetails(metaAccountId: Long) = commonNavigator.openAccountDetails(metaAccountId)

    override fun openCustomRebond() {
        performNavigation(R.id.action_stakingFragment_to_customRebondFragment)
    }

    override fun openCurrentValidators() {
        performNavigation(R.id.action_stakingFragment_to_currentValidatorsFragment)
    }

    override fun returnToCurrentValidators() {
        performNavigation(R.id.action_confirmStakingFragment_back_to_currentValidatorsFragment)
    }

    override fun openChangeRewardDestination() {
        performNavigation(R.id.action_stakingFragment_to_selectRewardDestinationFragment)
    }

    override fun openConfirmRewardDestination(payload: ConfirmRewardDestinationPayload) {
        performNavigation(
            R.id.action_selectRewardDestinationFragment_to_confirmRewardDestinationFragment,
            ConfirmRewardDestinationFragment.getBundle(payload)
        )
    }

    override fun openControllerAccount() {
        performNavigation(R.id.action_stakingBalanceFragment_to_setControllerAccountFragment)
    }

    override fun openConfirmSetController(payload: ConfirmSetControllerPayload) {
        performNavigation(
            R.id.action_stakingSetControllerAccountFragment_to_confirmSetControllerAccountFragment,
            ConfirmSetControllerFragment.getBundle(payload)
        )
    }

    override fun openRecommendedValidators() {
        performNavigation(R.id.action_startChangeValidatorsFragment_to_recommendedValidatorsFragment)
    }

    override fun openSelectCustomValidators() {
        performNavigation(R.id.action_startChangeValidatorsFragment_to_selectCustomValidatorsFragment)
    }

    override fun openCustomValidatorsSettings() {
        performNavigation(R.id.action_selectCustomValidatorsFragment_to_settingsCustomValidatorsFragment)
    }

    override fun openSearchCustomValidators() {
        performNavigation(R.id.action_selectCustomValidatorsFragment_to_searchCustomValidatorsFragment)
    }

    override fun openReviewCustomValidators() {
        performNavigation(R.id.action_selectCustomValidatorsFragment_to_reviewCustomValidatorsFragment)
    }

    override fun openConfirmStaking() {
        performNavigation(R.id.openConfirmStakingFragment)
    }

    override fun openConfirmNominations() {
        performNavigation(R.id.action_confirmStakingFragment_to_confirmNominationsFragment)
    }

    override fun openChainStakingMain() = performNavigation(R.id.action_mainFragment_to_stakingGraph)

    override fun openStartStakingLanding(chainId: ChainId, assetId: ChainAssetId, stakingTypes: List<Chain.Asset.StakingType>) {
        val bundle = StartStakingLandingFragment.getBundle(StartStakingLandingPayload(chainId, assetId, stakingTypes))
        performNavigation(R.id.action_mainFragment_to_startStackingLanding, bundle)
    }

    override fun openSetupStaking() {
        performNavigation(R.id.action_stakingFragment_to_setupStakingFragment)
    }

    override fun openStartChangeValidators() {
        performNavigation(R.id.openStartChangeValidatorsFragment)
    }

    override fun openStory(story: StakingStoryModel) {
        performNavigation(R.id.open_staking_story, StoryFragment.getBundle(story))
    }

    override fun openPayouts() {
        performNavigation(R.id.action_stakingFragment_to_payoutsListFragment)
    }

    override fun openPayoutDetails(payout: PendingPayoutParcelable) {
        performNavigation(R.id.action_payoutsListFragment_to_payoutDetailsFragment, PayoutDetailsFragment.getBundle(payout))
    }

    override fun openConfirmPayout(payload: ConfirmPayoutPayload) {
        performNavigation(R.id.action_open_confirm_payout, ConfirmPayoutFragment.getBundle(payload))
    }

    override fun openBondMore() {
        performNavigation(R.id.action_open_selectBondMoreFragment, SelectBondMoreFragment.getBundle(SelectBondMorePayload()))
    }

    override fun openConfirmBondMore(payload: ConfirmBondMorePayload) {
        performNavigation(R.id.action_selectBondMoreFragment_to_confirmBondMoreFragment, ConfirmBondMoreFragment.getBundle(payload))
    }

    override fun openSelectUnbond() {
        performNavigation(R.id.action_stakingFragment_to_selectUnbondFragment)
    }

    override fun openConfirmUnbond(payload: ConfirmUnbondPayload) {
        performNavigation(R.id.action_selectUnbondFragment_to_confirmUnbondFragment, ConfirmUnbondFragment.getBundle(payload))
    }

    override fun openRedeem() {
        performNavigation(R.id.action_open_redeemFragment, RedeemFragment.getBundle(RedeemPayload()))
    }

    override fun openConfirmRebond(payload: ConfirmRebondPayload) {
        performNavigation(R.id.action_open_confirm_rebond, ConfirmRebondFragment.getBundle(payload))
    }

    override fun openValidatorDetails(payload: StakeTargetDetailsPayload) {
        performNavigation(R.id.open_validator_details, ValidatorDetailsFragment.getBundle(payload))
    }

    override fun openRebag() = performNavigation(R.id.action_stakingFragment_to_rebag)
    override fun openDAppBrowser(url: String) = performNavigation(
        actionId = R.id.action_mainFragment_to_dappBrowserGraph,
        args = DAppBrowserFragment.getBundle(url)
    )

    override fun openMoreStakingOptions() {
        stakingTabNavController?.performNavigation(R.id.action_stakingDashboardFragment_to_moreStakingOptionsFragment)
    }

    override fun backInStakingTab() {
        stakingTabNavController?.popBackStack()
    }

    override fun openStakingPeriods() {
        performNavigation(R.id.action_stakingFragment_to_staking_periods)
    }

    override fun openSetupStakingType() {
        performNavigation(R.id.action_stakingLandingFragment_to_setupStakingTypeFragment)
    }
}
