package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.current

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegationState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import java.math.BigInteger

class DelegatedCollator(
    val collator: Collator,
    val delegation: BigInteger,
    val delegationStatus: DelegationState
)

sealed class DelegatedCollatorGroup(val numberOfCollators: Int, val position: Int) {
    companion object {
        val COMPARATOR = Comparator.comparingInt<DelegatedCollatorGroup> { it.position }
    }

    class Active(numberOfCollators: Int) : DelegatedCollatorGroup(numberOfCollators, 0)
    class Elected(numberOfCollators: Int) : DelegatedCollatorGroup(numberOfCollators, 1)
    class Inactive(numberOfCollators: Int) : DelegatedCollatorGroup(numberOfCollators, 2)
    class WaitingForNextEra(numberOfCollators: Int) : DelegatedCollatorGroup(numberOfCollators, 3)
}
