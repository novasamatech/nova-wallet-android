package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator

enum class CollatorSorting(private val collatorComparator: Comparator<Collator>) : Comparator<Collator> by collatorComparator {

    REWARDS(compareByDescending { it.apr }),
    MIN_STAKE(compareBy { it.minimumStakeToGetRewards }),
    TOTAL_STAKE(compareByDescending { it.snapshot?.total.orZero() }),
    OWN_STAKE(compareByDescending { it.snapshot?.bond.orZero() })
}

data class CollatorRecommendationConfig(val sorting: CollatorSorting): Comparator<Collator> by sorting {

    companion object {

        val DEFAULT = CollatorRecommendationConfig(CollatorSorting.REWARDS)
    }
}
