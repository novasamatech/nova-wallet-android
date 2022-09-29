package io.novafoundation.nova.feature_vote.presentation

import androidx.fragment.app.Fragment

interface VoteRouter {

    interface Factory {

        fun create(host: Fragment): VoteRouter
    }

    fun openDemocracy()

    fun openCrowdloans()
}
