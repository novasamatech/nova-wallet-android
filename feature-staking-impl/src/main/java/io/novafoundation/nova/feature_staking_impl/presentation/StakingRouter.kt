package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.pools.common.SelectingPoolPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.add.confirm.ConfirmAddStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.revoke.ConfirmRemoveStakingProxyPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import kotlinx.coroutines.flow.Flow

interface StakingRouter {

    fun openChainStakingMain()

    /**
     * Routes the dashboard tap on a Bittensor (`SUBTENSOR`) row to its own
     * detail fragment instead of the compound-component `StakingFragment`.
     * Subtensor doesn't fit the relaychain/parachain/Mythos component model.
     */
    fun openSubtensorStakingMain()

    /**
     * Subtensor stake-add type picker (Root TAO vs Subnet alpha). Pushed
     * from the Subtensor staking detail screen when the user taps
     * "Stake more". Mirrors iOS `SubtensorStakingWireframe.showStakingFlow`.
     */
    fun openSubtensorStakeType()

    /**
     * Subnet alpha picker. Pushed from the Type Picker when the user picks
     * Subnet on Continue. Selecting a row routes onward to the stake setup
     * screen with that netuid.
     */
    fun openSubtensorSubnetPicker()

    /**
     * Subtensor stake-add amount + validator setup screen. Pushed either
     * from the Type Picker (Root path, netuid = 0) or from the Subnet
     * Picker (subnet path, netuid 1..128). `subnetName` is rendered in the
     * setup screen header for subnet flows; null for root.
     */
    fun openSubtensorStakeSetup(netuid: Int, subnetName: String? = null)

    /**
     * Subtensor validator picker. Pushed from the stake-setup screen's
     * "Select validator" slot. Selection is reported back via
     * [SubtensorStakeSetupFragment.KEY_SELECTED_VALIDATOR_HOTKEY] on the
     * setup screen's saved state handle.
     */
    fun openSubtensorValidatorPicker(netuid: Int)

    /**
     * Writes the picked hotkey into the previous back-stack entry's saved
     * state handle and pops the validator picker. Same idiom as the
     * crowdloan-bonus round-trip (see `Navigator.setCustomBonus`).
     */
    fun respondAndPopValidatorPicker(hotkey: String)

    /**
     * Stream of the most-recently-picked Subtensor validator hotkey, scoped
     * to the current Stake Setup back-stack entry. Mirrors the crowdloan
     * `customBonusFlow` pattern.
     */
    val subtensorSelectedValidatorFlow: Flow<String?>

    /**
     * Subtensor unstake setup screen. Pushed from the main TAO staking
     * screen — the user has already picked a position (or there's only
     * one), so the destination receives a fully-resolved
     * (netuid, hotkey, position-amount) triple. Mirrors iOS
     * `SubtensorStakingWireframe.pushUnstakeSetup`.
     */
    fun openSubtensorUnstakeSetup(netuid: Int, hotkeyAddress: String, positionPlanks: java.math.BigInteger)

    fun openStartChangeValidators()

    fun openRecommendedValidators()

    fun openSelectCustomValidators()

    fun openCustomValidatorsSettings()

    fun openSearchCustomValidators()

    fun openReviewCustomValidators(payload: CustomValidatorsPayload)

    fun openValidatorDetails(payload: StakeTargetDetailsPayload)

    fun openConfirmStaking()

    fun openConfirmNominations()

    fun returnToStakingMain()

    fun openSwitchWallet()

    fun openPayouts()

    fun openPayoutDetails(payout: PendingPayoutParcelable)

    fun openConfirmPayout(payload: ConfirmPayoutPayload)

    fun openBondMore()

    fun openConfirmBondMore(payload: ConfirmBondMorePayload)

    fun openSelectUnbond()

    fun openConfirmUnbond(payload: ConfirmUnbondPayload)

    fun openRedeem()

    fun openControllerAccount()

    fun back()

    fun openConfirmSetController(payload: ConfirmSetControllerPayload)

    fun openCustomRebond()
    fun openConfirmRebond(payload: ConfirmRebondPayload)

    fun openCurrentValidators()

    fun returnToCurrentValidators()

    fun openChangeRewardDestination()

    fun openConfirmRewardDestination(payload: ConfirmRewardDestinationPayload)

    fun openWalletDetails(metaAccountId: Long)

    fun openRebag()

    fun openStakingPeriods()

    fun openSetupStakingType()

    fun openSelectPool(payload: SelectingPoolPayload)

    fun openSearchPool(payload: SelectingPoolPayload)

    fun finishSetupValidatorsFlow()

    fun finishSetupPoolFlow()

    fun finishRedeemFlow(redeemConsequences: RedeemConsequences)

    fun openAddStakingProxy()

    fun openConfirmAddStakingProxy(payload: ConfirmAddStakingProxyPayload)

    fun openStakingProxyList()

    fun openConfirmRemoveStakingProxy(payload: ConfirmRemoveStakingProxyPayload)

    fun openDAppBrowser(url: String)

    fun openStakingDashboard()
}
