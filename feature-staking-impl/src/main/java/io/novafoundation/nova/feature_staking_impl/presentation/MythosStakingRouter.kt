package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemConsequences
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

interface MythosStakingRouter : StarkingReturnableRouter {

    fun openCollatorDetails(payload: StakeTargetDetailsPayload)

    fun openConfirmStartStaking(payload: ConfirmStartMythosStakingPayload)

    fun openClaimRewards()

    fun returnToStartStaking()

    fun openBondMore()

    fun openUnbond()

    fun openUnbondConfirm(payload: ConfirmUnbondMythosPayload)

    fun openRedeem()

    fun finishRedeemFlow(redeemConsequences: RedeemConsequences)
}
