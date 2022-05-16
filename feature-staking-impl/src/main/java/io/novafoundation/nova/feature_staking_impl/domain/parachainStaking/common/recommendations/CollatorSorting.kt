package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations

import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator

enum class CollatorSorting(private val collatorComparator: Comparator<Collator>) : Comparator<Collator> by collatorComparator {

    REWARDS(compareByDescending { it.apr }),
    MIN_STAKE(compareBy { it.minimumStakeToGetRewards }),
    TOTAL_STAKE(compareByDescending { it.snapshot.total }),
    OWN_STAKE(compareByDescending { it.snapshot.bond })
}


data class CollatorRecommendationConfig(val sorting: CollatorSorting) {

    companion object {

        val DEFAULT = CollatorRecommendationConfig(CollatorSorting.REWARDS)
    }
}
