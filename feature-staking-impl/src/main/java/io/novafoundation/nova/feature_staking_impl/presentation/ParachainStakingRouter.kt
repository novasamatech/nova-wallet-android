package io.novafoundation.nova.feature_staking_impl.presentation

import androidx.lifecycle.Lifecycle
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ParachainStakingRouter {

    fun openStartStaking()
    fun openConfirmStartStaking(payload: ConfirmStartParachainStakingPayload)
    fun openSearchCollator()

    fun openAddAccount(chainId: ChainId, metaId: Long)

    fun back()
    fun returnToMain()
    fun returnToStartStaking()

    val currentStackEntryLifecycle: Lifecycle
}
