package io.novafoundation.nova.feature_vote.presentation.vote

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VoteViewModel(
    private val voteRouter: VoteRouter,
): BaseViewModel() {

    fun crowdloansSelected() {
        voteRouter.openCrowdloans()
    }

    fun democracySelected() {
        voteRouter.openDemocracy()
    }
}
