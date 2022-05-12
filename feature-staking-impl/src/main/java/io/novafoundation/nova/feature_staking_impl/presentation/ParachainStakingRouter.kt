package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ParachainStakingRouter {

    fun openStartStaking()

    fun openAddAccount(chainId: ChainId, metaId: Long)
}
