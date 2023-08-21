package io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.model

import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingTarget
import java.math.BigInteger

sealed interface EditableStakingType {

    val isSelected: Boolean

    val isAvailable: Boolean

    val minStakeAmount: BigInteger

    val payoutType: PayoutType

    val reusableInGovernance: Boolean

    val advancedOptionsAvailable: Boolean

    val stakingTarget: StakingTarget

    class PoolStaking(
        override val isSelected: Boolean,
        override val isAvailable: Boolean,
        override val minStakeAmount: BigInteger,
        override val stakingTarget: StakingTarget.NominationPool
    ) : EditableStakingType {

        override val payoutType: PayoutType = PayoutType.Manual

        override val reusableInGovernance: Boolean = false

        override val advancedOptionsAvailable: Boolean = false
    }

    class DirectStaking(
        override val isSelected: Boolean,
        override val isAvailable: Boolean,
        override val minStakeAmount: BigInteger,
        override val payoutType: PayoutType,
        override val stakingTarget: StakingTarget.Validators
    ) : EditableStakingType {

        override val reusableInGovernance: Boolean = false

        override val advancedOptionsAvailable: Boolean = false
    }
}
