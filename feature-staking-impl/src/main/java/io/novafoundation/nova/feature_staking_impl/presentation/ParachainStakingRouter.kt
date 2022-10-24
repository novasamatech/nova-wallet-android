package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond.model.ParachainStakingRebondPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload

interface ParachainStakingRouter {

    fun openStartStaking()
    fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload)
    fun openSearchCollator()

    fun openCollatorDetails(payload: StakeTargetDetailsPayload)

    fun openWalletDetails(metaId: Long)

    fun back()
    fun returnToMain()
    fun returnToStartStaking()

    fun openCurrentCollators()

    fun openUnbond()
    fun openConfirmUnbond(payload: ParachainStakingUnbondConfirmPayload)

    fun openRedeem()

    fun openRebond(payload: ParachainStakingRebondPayload)

    fun openSetupYieldBoost()
    fun openConfirmYieldBoost(payload: YieldBoostConfirmPayload)
}
