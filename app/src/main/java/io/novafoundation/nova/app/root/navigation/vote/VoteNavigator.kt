package io.novafoundation.nova.app.root.navigation.vote

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.common.utils.onDestroy
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.CrowdloanFragment
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VoteNavigatorFactory(
    private val commonNavigator: Navigator
): VoteRouter.Factory {

    override fun create(host: Fragment): VoteRouter {
        return VoteNavigator(host, commonNavigator)
    }
}

private const val INDEX_DEMOCRACY = 0
private const val INDEX_CROWDLOANS = 1

private class VoteNavigator(
    private val host: Fragment,
    private val commonNavigator: Navigator,
) : VoteRouter {

    override fun openDemocracy() {
        openTabAt(INDEX_DEMOCRACY)
    }

    override fun openCrowdloans() {
        openTabAt(INDEX_CROWDLOANS)
    }

    override fun openSwitchWallet() {
        commonNavigator.openSwitchWallet()
    }

    override fun openTabAt(index: Int) {
        val fragment = when (index) {
            INDEX_DEMOCRACY -> Fragment()
            INDEX_CROWDLOANS -> CrowdloanFragment()
            else -> error("Unknown index: $index")
        }

        replaceFragment(fragment)
    }

    override fun listenCurrentTab(lifecycle: Lifecycle, onChange: (index: Int) -> Unit) {
        val listener = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                onChange(f.tabIndex)
            }
        }

        host.childFragmentManager.registerFragmentLifecycleCallbacks(listener, false)

        lifecycle.onDestroy {
            host.childFragmentManager.unregisterFragmentLifecycleCallbacks(listener)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        host.childFragmentManager.beginTransaction()
            .replace(R.id.voteFragmentContainer, fragment)
            .commit()
    }

    private val Fragment.tabIndex: Int
        get() = when (this) {
            is CrowdloanFragment -> INDEX_CROWDLOANS
            else -> INDEX_DEMOCRACY
        }
}
