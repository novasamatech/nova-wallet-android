package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ParachainStakingRouter {

    fun openStartStaking()
    fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload)

    fun openAddAccount(chainId: ChainId, metaId: Long)

    fun back()
    fun returnToMain()
}
