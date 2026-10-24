package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorFlowExit
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.BittensorPortfolioPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing.model.StartStakingLandingPayload

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSelectionState

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorChooseSubnetPayload

import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.amount.BittensorStakeAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.claim.BittensorRootClaimPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.BittensorStakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitReviewPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position.BittensorSubnetPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetDetailsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator.BittensorValidatorInfoPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewPayload
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink.StakingDeepLinkTarget
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

interface StakingRouter {

    fun openChainStakingMain()

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

    fun openStakingDashboard(target: StakingDeepLinkTarget? = null)

    fun openBittensorExitAmount(position: BittensorExitPositionPayload)

    fun openBittensorExitReview(review: BittensorExitReviewPayload)

    fun openBittensorRootClaim(payload: BittensorRootClaimPayload)

    fun openBittensorRootStaking(
        payload: BittensorStakeAmountPayload? = null,
        selectionState: BittensorSelectionState = BittensorSelectionState()
    )

    fun openBittensorPortfolio(payload: BittensorPortfolioPayload)

    /** Opens the Bittensor Earn landing on top of the current screen ("Add position" on the portfolio). */
    fun openBittensorStartStaking(payload: StartStakingLandingPayload)

    /** Leaves a finished Bittensor operation, dropping its flow screens from the back stack. */
    fun finishBittensorFlow(exit: BittensorFlowExit)

    fun openBittensorRootDetails()

    fun openBittensorStakeConfirm(payload: BittensorStakeConfirmPayload)

    fun openBittensorChooseSubnet(payload: BittensorChooseSubnetPayload = BittensorChooseSubnetPayload())

    fun replaceBittensorSubnetAmountWithRoot(
        selectionState: BittensorSelectionState
    )

    fun openBittensorWhereToStake()

    fun openBittensorSubnetDetails(payload: BittensorSubnetDetailsPayload)

    fun openBittensorValidatorInfo(payload: BittensorValidatorInfoPayload)

    fun openBittensorSubnetPosition(payload: BittensorSubnetPositionPayload)

    fun openBittensorSubnetAmount(payload: BittensorSubnetAmountPayload)

    fun openBittensorSubnetReview(payload: BittensorSubnetReviewPayload)
}
