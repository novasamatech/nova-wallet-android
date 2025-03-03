package io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model

import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class CurrentMythosCollator(
    val collator: MythosCollator,
    val userStake: Balance,
    val status: MythosDelegationStatus,
)
