package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLockId

object MythosStakingFreezeIds {

    val STAKING = BalanceLockId.fromPath(Modules.COLLATOR_STAKING, "Staking")

    val RELEASING = BalanceLockId.fromPath(Modules.COLLATOR_STAKING, "Releasing")
}
