package io.novafoundation.nova.feature_vote.presentation

import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.view.tabs.TabsRouter

interface VoteRouter: TabsRouter {

    interface Factory {

        fun create(host: Fragment): VoteRouter
    }

    fun openDemocracy()

    fun openCrowdloans()
}
