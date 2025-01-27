package io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations

import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator

enum class MythosCollatorSorting(private val collatorComparator: Comparator<MythosCollator>) : Comparator<MythosCollator> by collatorComparator {

    REWARDS(compareByDescending { it.apr }),
    TOTAL_STAKE(compareByDescending { it.totalStake }),
}

data class MythosCollatorRecommendationConfig(val sorting: MythosCollatorSorting) : Comparator<MythosCollator> by sorting {

    companion object {

        val DEFAULT = MythosCollatorRecommendationConfig(MythosCollatorSorting.REWARDS)
    }
}
