package io.novafoundation.nova.feature_vote.presentation

import androidx.fragment.app.Fragment

interface VoteRouter {

    fun getDemocracyFragment(): Fragment

    fun getCrowdloansFragment(): Fragment

    fun openSwitchWallet()
}
