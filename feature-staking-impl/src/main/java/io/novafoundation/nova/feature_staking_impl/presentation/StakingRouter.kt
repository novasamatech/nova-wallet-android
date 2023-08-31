package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.payouts.confirm.model.ConfirmPayoutPayload
import io.novafoundation.nova.feature_staking_impl.presentation.payouts.model.PendingPayoutParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.confirm.ConfirmBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.controller.confirm.ConfirmSetControllerPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rewardDestination.confirm.parcel.ConfirmRewardDestinationPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm.ConfirmUnbondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

interface StakingRouter {

    fun openChainStakingMain()

    fun openSetupStaking()

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

    fun openStory(story: StakingStoryModel)

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

    fun openAccountDetails(metaAccountId: Long)

    fun openRebag()

    fun openDAppBrowser(url: String)

    fun openStakingPeriods()

    fun openSetupStakingType()

    fun finishSetupValidatorsFlow()
}
