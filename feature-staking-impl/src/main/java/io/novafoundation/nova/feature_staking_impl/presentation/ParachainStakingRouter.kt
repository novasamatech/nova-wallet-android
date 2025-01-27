package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.StartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

interface ParachainStakingRouter : StarkingReturnableRouter {

    fun openStartStaking(payload: StartParachainStakingPayload)

    fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload)

    fun openSearchCollator()

    fun openCollatorDetails(payload: StakeTargetDetailsPayload)

    fun openWalletDetails(metaId: Long)

    fun openCurrentCollators()

    fun openUnbond()
    fun openConfirmUnbond(payload: ParachainStakingUnbondConfirmPayload)

    fun openRedeem()

    fun openRebond(payload: ParachainStakingRebondPayload)

    fun openSetupYieldBoost()
    fun openConfirmYieldBoost(payload: YieldBoostConfirmPayload)

    fun openAddStakingProxy()
}

fun ParachainStakingRouter.openStartStaking(flowMode: StartParachainStakingMode) = openStartStaking(StartParachainStakingPayload(flowMode))
