package io.novafoundation.nova.feature_staking_impl.domain.mythos.currentCollators.model

import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isActive

enum class MythosDelegationStatus {

    ACTIVE, NOT_ACTIVE
}

fun MythosCollator.delegationStatus(): MythosDelegationStatus {
    return if (isActive) MythosDelegationStatus.ACTIVE else MythosDelegationStatus.NOT_ACTIVE
}
