package io.novafoundation.nova.app.root.navigation.vote

import androidx.fragment.app.Fragment
import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanFragment
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VoteNavigatorFactory : VoteRouter.Factory {

    override fun create(host: Fragment): VoteRouter {
        return VoteNavigator(host)
    }
}

private class VoteNavigator(
    private val host: Fragment
): VoteRouter {

    override fun openDemocracy() {
        val stub = Fragment()

        replaceFragment(stub)
    }

    override fun openCrowdloans() {
        val crowdloansFragment = CrowdloanFragment()

        replaceFragment(crowdloansFragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        host.childFragmentManager.beginTransaction()
            .replace(R.id.voteFragmentContainer, fragment)
            .commit()
    }
}
